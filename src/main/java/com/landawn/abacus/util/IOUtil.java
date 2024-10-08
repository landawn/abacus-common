/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn.BiPredicates;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Apache Commons IO, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 * <br />
 * There are only {@code offset/count/len} parameters in the methods defined in class {@codeIOUtil}, no {@code fromIndex/startIndex} and {toIndex/endIndex} parameters.
 *
 * @see java.nio.file.Files
 * @see com.landawn.abacus.guava.Files
 * @see com.landawn.abacus.util.Strings
 *
 * @version $Revision: 0.8 $
 */
public final class IOUtil {

    private static final Logger logger = LoggerFactory.getLogger(IOUtil.class);

    // Keep it consistent for those methods specified or not specified charset by using the default jvm charset. Should it be UTF-8?  TODO
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset(); // StandardCharsets.UTF_8;

    // ...
    private static final String JAVA_VENDOR_STR = "java.vendor";

    private static final String JAVA_VM_VENDOR_STR = "java.vm.vendor";

    private static final String ANDROID = "ANDROID";

    private static final String ZIP = ".zip";

    private static final String GZ = ".gz";

    // ...
    private static final Splitter pathSplitter = Splitter.with('/').trimResults();

    /** The file copy buffer size (32 MB). */
    private static final int FILE_COPY_BUFFER_SIZE = (1024 * 1024) * 32;

    static final Method stringEncodeMethod;

    static final Method stringDecodeMethod;

    static {
        Method encodeMethod = null;
        Method decodeMethod = null;

        try {
            final Class<?> cls = ClassUtil.forClass("java.lang.StringCoding");
            final Method enMethod = ClassUtil.getDeclaredMethod(cls, "encode", Charset.class, char[].class, int.class, int.class);
            final Method deMethod = ClassUtil.getDeclaredMethod(cls, "decode", Charset.class, byte[].class, int.class, int.class);

            if (enMethod != null && deMethod != null) {
                ClassUtil.setAccessible(enMethod, true);
                ClassUtil.setAccessible(deMethod, true);

                final char[] chars = "abc".toCharArray();
                final byte[] bytes = ClassUtil.invokeMethod(enMethod, DEFAULT_CHARSET, chars, 1, 1);
                final char[] chars2 = ClassUtil.invokeMethod(deMethod, DEFAULT_CHARSET, bytes, 0, bytes.length);

                if (chars2.length == 1 && chars2[0] == 'b') {
                    encodeMethod = enMethod;
                    decodeMethod = deMethod;
                }
            }

        } catch (final Exception e) {
            // e.printStackTrace();
            // ignore
        }

        stringEncodeMethod = encodeMethod;
        stringDecodeMethod = decodeMethod;
    }

    private static String hostName;

    public static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    public static final long ONE_KB = 1024;

    public static final long ONE_MB = 1024 * ONE_KB;

    public static final long ONE_GB = 1024 * ONE_MB;

    public static final long ONE_TB = 1024 * ONE_GB;

    public static final long ONE_PB = 1024 * ONE_TB;

    public static final long ONE_EB = 1024 * ONE_PB;

    public static final long ONE_ZB = 1024 * ONE_EB;

    public static final int MAX_MEMORY_IN_MB = (int) (Runtime.getRuntime().maxMemory() / (1024 * 1024));

    // ...
    public static final String OS_NAME = System.getProperty("os.name");

    public static final String OS_VERSION = System.getProperty("os.version");

    public static final String OS_ARCH = System.getProperty("os.arch");

    //...
    public static final boolean IS_OS_WINDOWS = OS_NAME.toUpperCase().contains("WINDOWS");

    public static final boolean IS_OS_MAC = OS_NAME.toUpperCase().contains("MAC");

    public static final boolean IS_OS_MAC_OSX = OS_NAME.toUpperCase().contains("MAC OS X");

    public static final boolean IS_OS_LINUX = OS_NAME.toUpperCase().contains("LINUX");

    public static final boolean IS_PLATFORM_ANDROID = System.getProperty(JAVA_VENDOR_STR).toUpperCase().contains(ANDROID)
            || System.getProperty(JAVA_VM_VENDOR_STR).toUpperCase().contains(ANDROID);

    // ...
    public static final String JAVA_HOME = System.getProperty("java.home");

    public static final JavaVersion JAVA_VERSION = JavaVersion.of(System.getProperty("java.version"));

    public static final String JAVA_VENDOR = System.getProperty(JAVA_VENDOR_STR);

    public static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");

    public static final String JAVA_CLASS_VERSION = System.getProperty("java.class.version");

    public static final String JAVA_RUNTIME_NAME = System.getProperty("java.runtime.name");

    public static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");

    public static final String JAVA_SPECIFICATION_NAME = System.getProperty("java.specification.name");

    public static final String JAVA_SPECIFICATION_VENDOR = System.getProperty("java.specification.vendor");

    public static final String JAVA_SPECIFICATION_VERSION = System.getProperty("java.specification.version");

    public static final String JAVA_VM_INFO = System.getProperty("java.vm.info");

    public static final String JAVA_VM_NAME = System.getProperty("java.vm.name");

    public static final String JAVA_VM_SPECIFICATION_NAME = System.getProperty("java.vm.specification.name");

    public static final String JAVA_VM_SPECIFICATION_VENDOR = System.getProperty("java.vm.specification.vendor");

    public static final String JAVA_VM_SPECIFICATION_VERSION = System.getProperty("java.vm.specification.version");

    public static final String JAVA_VM_VENDOR = System.getProperty(JAVA_VM_VENDOR_STR);

    public static final String JAVA_VM_VERSION = System.getProperty("java.vm.version");

    public static final String JAVA_IO_TMPDIR = System.getProperty("java.io.tmpdir");

    static final String JAVA_VENDOR_URL = System.getProperty("java.vendor.url");

    static final String JAVA_LIBRARY_PATH = System.getProperty("java.library.path");

    static final String JAVA_COMPILER = System.getProperty("java.compiler");

    static final String JAVA_ENDORSED_DIRS = System.getProperty("java.endorsed.dirs");

    static final String JAVA_EXT_DIRS = System.getProperty("java.ext.dirs");

    // ...
    static final String JAVA_AWT_FONTS = System.getProperty("java.awt.fonts");

    static final String JAVA_AWT_GRAPHICSENV = System.getProperty("java.awt.graphicsenv");

    static final String JAVA_AWT_HEADLESS = System.getProperty("java.awt.headless");

    static final String JAVA_AWT_PRINTERJOB = System.getProperty("java.awt.printerjob");

    static final String JAVA_UTIL_PREFS_PREFERENCES_FACTORY = System.getProperty("java.util.prefs.PreferencesFactory");

    // ...
    public static final String USER_DIR = System.getProperty("user.dir");

    public static final String USER_HOME = System.getProperty("user.home");

    public static final String USER_NAME = System.getProperty("user.name");

    public static final String USER_TIMEZONE = System.getProperty("user.timezone");

    public static final String USER_LANGUAGE = System.getProperty("user.language");

    public static final String USER_COUNTRY = System.getProperty("user.country") == null ? System.getProperty("user.region")
            : System.getProperty("user.country");

    /**
     * @see File#pathSeparator
     */
    public static final String PATH_SEPARATOR = File.pathSeparator;

    /**
     * The system directory separator character.
     * @see File#separator
     */
    public static final String DIR_SEPARATOR = File.separator;

    /**
     * The Unix directory separator character.
     */
    public static final String DIR_SEPARATOR_UNIX = "/";

    /**
     * The Windows directory separator character.
     */
    public static final String DIR_SEPARATOR_WINDOWS = "\\";

    /**
     * @see System#lineSeparator()
     */
    public static final String LINE_SEPARATOR = System.lineSeparator();

    /** current path retrieved by {@code new File("./").getAbsolutePath()} */
    public static final String CURRENT_DIR;

    static {
        final String path = new File("./").getAbsolutePath();
        CURRENT_DIR = path.charAt(path.length() - 1) == '.' ? path.substring(0, path.length() - 1) : path;
    }

    // ...
    public static final int EOF = -1;

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> all_files_filter = (parentDir, file) -> true;

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> directories_excluded_filter = (parentDir, file) -> !file.isDirectory();

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> directories_only_filter = (parentDir, file) -> file.isDirectory();

    /**
     * Constructor for FileUtil.
     */
    private IOUtil() {
        // no instance;
    }

    /**
     *
     *
     * @return
     */
    public static String getHostName() {
        String ret = hostName;

        if (ret == null) {
            // This may be slow on some machine. Move it from static initialization block to method.

            final boolean IS_PLATFORM_ANDROID = System.getProperty(JAVA_VENDOR_STR).toUpperCase().contains(ANDROID) //NOSONAR
                    || System.getProperty(JAVA_VM_VENDOR_STR).toUpperCase().contains(ANDROID); //NOSONAR

            // implementation for android support
            if (IS_PLATFORM_ANDROID) {
                try {
                    ret = Executors.newSingleThreadExecutor().submit(() -> InetAddress.getLocalHost().getHostName()).get();
                } catch (final Exception e) {
                    logger.error("Failed to get host name");
                }
            } else {
                try {
                    ret = InetAddress.getLocalHost().getHostName();
                } catch (final Exception e) {
                    logger.error("Failed to get host name");
                }
            }

            if (ret == null) {
                ret = "UNKNOWN_HOST_NAME";
            }

            hostName = ret;
        }

        return ret;
    }

    /**
     * Returns the disk size of the volume which holds the working directory.
     * <p>
     * Identical to:
     * <pre>
     * freeSpaceKb(new File(".").getAbsolutePath())
     * </pre>
     *
     * @return
     * @throws UncheckedIOException if an error occurs when finding the free space
     * @throws IllegalStateException if an error occurred in initialisation
     */
    public static long freeDiskSpaceKb() throws UncheckedIOException {
        try {
            return FileSystemUtil.freeSpaceKb();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the disk size of the volume which holds the working directory.
     * <p>
     * Identical to:
     * <pre>
     * freeSpaceKb(new File(".").getAbsolutePath())
     * </pre>
     *
     * @param timeout The timeout amount in milliseconds or no timeout if the value
     *  is zero or less
     * @return
     * @throws UncheckedIOException if an error occurs when finding the free space
     * @throws IllegalStateException if an error occurred in initialisation
     */
    public static long freeDiskSpaceKb(final long timeout) throws UncheckedIOException {
        try {
            return FileSystemUtil.freeSpaceKb(timeout);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the free space on a drive or volume in kilobytes by invoking
     * the command line.
     * <pre>
     * FileSystemUtils.freeSpaceKb("C:");       // Windows
     * FileSystemUtils.freeSpaceKb("/volume");  // *nix
     * </pre>
     * The free space is calculated via the command line.
     * It uses 'dir /-c' on Windows, 'df -kP' on AIX/HP-UX and 'df -k' on other Unix.
     * <p>
     * In order to work, you must be running Windows, or have a implementation of
     * Unix df that supports GNU format when passed -k (or -kP). If you are going
     * to rely on this code, please check that it works on your OS by running
     * some simple tests to compare the command line with the output from this class.
     * If your operating system isn't supported, please raise a JIRA call detailing
     * the exact result from df -k and as much other detail as possible, thanks.
     *
     * @param path the path to get free space for, not null, not empty on Unix
     * @return
     * @throws UncheckedIOException if an error occurs when finding the free space
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialisation
     */
    public static long freeDiskSpaceKb(final String path) throws UncheckedIOException {
        try {
            return FileSystemUtil.freeSpaceKb(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the free space on a drive or volume in kilobytes by invoking
     * the command line.
     * <pre>
     * FileSystemUtils.freeSpaceKb("C:");       // Windows
     * FileSystemUtils.freeSpaceKb("/volume");  // *nix
     * </pre>
     * The free space is calculated via the command line.
     * It uses 'dir /-c' on Windows, 'df -kP' on AIX/HP-UX and 'df -k' on other Unix.
     * <p>
     * In order to work, you must be running Windows, or have a implementation of
     * Unix df that supports GNU format when passed -k (or -kP). If you are going
     * to rely on this code, please check that it works on your OS by running
     * some simple tests to compare the command line with the output from this class.
     * If your operating system isn't supported, please raise a JIRA call detailing
     * the exact result from df -k and as much other detail as possible, thanks.
     *
     * @param path the path to get free space for, not null, not empty on Unix
     * @param timeout The timeout amount in milliseconds or no timeout if the value
     *  is zero or less
     * @return
     * @throws UncheckedIOException if an error occurs when finding the free space
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialisation
     */
    public static long freeDiskSpaceKb(final String path, final long timeout) throws UncheckedIOException {
        try {
            return FileSystemUtil.freeSpaceKb(path, timeout);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Charset checkCharset(final Charset charset) {
        return charset == null ? DEFAULT_CHARSET : charset;
    }

    /**
     * Chars 2 bytes.
     *
     * @param chars
     * @return
     */
    public static byte[] chars2Bytes(final char[] chars) {
        return chars2Bytes(chars, DEFAULT_CHARSET);
    }

    /**
     * Chars 2 bytes.
     *
     * @param chars
     * @param charset
     * @return
     */
    public static byte[] chars2Bytes(final char[] chars, final Charset charset) {
        if (N.isEmpty(chars)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return chars2Bytes(chars, 0, chars.length, charset);
    }

    /**
     * Chars 2 bytes.
     *
     * @param chars
     * @param offset
     * @param charCount
     * @param charset
     * @return
     */
    public static byte[] chars2Bytes(final char[] chars, final int offset, final int charCount, Charset charset) {
        if (charCount == 0 && N.len(chars) >= offset) {
            return N.EMPTY_BYTE_ARRAY;
        }

        charset = checkCharset(charset);

        if (stringEncodeMethod == null) {
            return new String(chars, offset, charCount).getBytes(charset);
        } else {
            return ClassUtil.invokeMethod(stringEncodeMethod, charset, chars, offset, charCount);
        }
    }

    /**
     * Bytes 2 chars.
     *
     * @param bytes
     * @return
     */
    public static char[] bytes2Chars(final byte[] bytes) {
        return bytes2Chars(bytes, DEFAULT_CHARSET);
    }

    /**
     * Bytes 2 chars.
     *
     * @param bytes
     * @param charset
     * @return
     */
    public static char[] bytes2Chars(final byte[] bytes, final Charset charset) {
        if (N.isEmpty(bytes)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return bytes2Chars(bytes, 0, bytes.length, charset);
    }

    /**
     * Bytes 2 chars.
     *
     * @param bytes
     * @param offset
     * @param byteCount
     * @param charset
     * @return
     */
    public static char[] bytes2Chars(final byte[] bytes, final int offset, final int byteCount, Charset charset) {
        if (byteCount == 0 && N.len(bytes) >= offset) {
            return N.EMPTY_CHAR_ARRAY;
        }

        charset = checkCharset(charset);

        if (stringDecodeMethod == null) {
            return new String(bytes, offset, byteCount, charset).toCharArray();
        } else {
            return ClassUtil.invokeMethod(stringDecodeMethod, charset, bytes, offset, byteCount);
        }
    }

    /**
     * String 2 input stream.
     *
     * @param str
     * @return
     */
    public static InputStream string2InputStream(final String str) {
        return string2InputStream(str, DEFAULT_CHARSET);
    }

    /**
     *
     * @param str
     * @param charset
     * @return
     */
    public static InputStream string2InputStream(final String str, Charset charset) {
        if (str == null) {
            throw new IllegalArgumentException("The input String can't be null.");
        }

        charset = checkCharset(charset);

        return new ByteArrayInputStream(str.getBytes(charset));
    }

    /**
     * String 2 reader.
     *
     * @param str
     * @return
     */
    public static Reader string2Reader(final String str) {
        if (str == null) {
            throw new IllegalArgumentException("The input String can't be null.");
        }

        return new StringReader(str);
    }

    /**
     * String builder 2 writer.
     *
     * @param sb
     * @return
     */
    public static Writer stringBuilder2Writer(final StringBuilder sb) {
        if (sb == null) {
            throw new IllegalArgumentException("The input StringBuilder can't be null.");
        }

        return new StringWriter(sb);
    }

    /**
     * Note: It should not be used to read {@code File/InputStream} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static byte[] readAllBytes(final File source) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readBytes(is, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static byte[] readAllBytes(final InputStream source) throws UncheckedIOException {
        try {
            return readBytes(source, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException the unchecked IO exception
     * @see #readAllBytes(File)
     */
    public static byte[] readBytes(final File source) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readBytes(is, 0, Long.MAX_VALUE);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(final File source, final long offset, final int maxLen) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readBytes(is, offset, maxLen);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException
     * @see #readAllBytes(InputStream)
     */
    public static byte[] readBytes(final InputStream source) throws IOException {
        return readBytes(source, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(final InputStream source, final long offset, final int maxLen) throws IOException {
        return readBytes(source, offset, (long) maxLen);
    }

    private static byte[] readBytes(final InputStream source, final long offset, final long maxLen) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        if ((maxLen == 0) || ((offset > 0) && (skip(source, offset) < offset))) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final byte[] buf = Objectory.createByteArrayBuffer();
        byte[] byteArray = buf;
        int arrayLength = byteArray.length;

        int count = 0;
        int cnt = 0;

        try {
            while (count < maxLen && EOF != (cnt = read(source, byteArray, count, (int) Math.min(maxLen - count, arrayLength - count)))) { // NOSONAR
                count += cnt;

                if (count < maxLen && count >= arrayLength) {
                    int newCapacity = (int) (arrayLength * 1.75);

                    if (newCapacity < 0 || newCapacity > maxLen || newCapacity > N.MAX_ARRAY_SIZE) {
                        newCapacity = (int) N.min(maxLen, N.MAX_ARRAY_SIZE);
                    }

                    if (newCapacity <= arrayLength) {
                        throw new OutOfMemoryError("Required array size too large");
                    }

                    byteArray = Arrays.copyOf(byteArray, newCapacity);
                    arrayLength = byteArray.length;
                }
            }

            return (count <= 0 ? N.EMPTY_BYTE_ARRAY : N.copyOfRange(byteArray, 0, count));

        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final File source) throws UncheckedIOException {
        return readAllChars(source, DEFAULT_CHARSET);
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final File source, final Charset encoding) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readAllChars(is, encoding);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final InputStream source) throws UncheckedIOException {
        return readAllChars(source, DEFAULT_CHARSET);
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final InputStream source, final Charset encoding) throws UncheckedIOException {
        final Reader reader = createReader(source, encoding);

        try {
            return readChars(reader, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final Reader source) throws UncheckedIOException {
        try {
            return readChars(source, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException
     * @see #readAllChars(File)
     */
    public static char[] readChars(final File source) throws IOException {
        return readChars(source, DEFAULT_CHARSET);
    }

    /**
     *
     * @param source
     * @param encoding
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException
     * @see #readAllChars(File, Charset)
     */
    public static char[] readChars(final File source, final Charset encoding) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readChars(is, encoding);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static char[] readChars(final File source, final long offset, final int maxLen) throws IOException {
        return readChars(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     *
     * @param source
     * @param encoding
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static char[] readChars(final File source, final Charset encoding, final long offset, final int maxLen) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readChars(is, encoding, offset, maxLen);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException
     * @see #readAllChars(InputStream)
     */
    public static char[] readChars(final InputStream source) throws IOException {
        return readChars(source, DEFAULT_CHARSET);
    }

    /**
     *
     * @param source
     * @param encoding
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException
     * @see #readAllChars(InputStream, encoding)
     */
    public static char[] readChars(final InputStream source, final Charset encoding) throws IOException {
        final Reader reader = createReader(source, encoding);

        //    try {
        return readChars(reader, 0, Long.MAX_VALUE);
        //    } finally {
        //        close(reader);
        //    }
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static char[] readChars(final InputStream source, final long offset, final int maxLen) throws IOException {
        return readChars(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     *
     * @param source
     * @param encoding
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static char[] readChars(final InputStream source, Charset encoding, final long offset, final int maxLen) throws IOException {
        encoding = checkCharset(encoding);

        // try {
        final Reader reader = createReader(source, encoding);
        // } finally {
        // // close(reader);
        // }

        return readChars(reader, offset, maxLen);
    }

    /**
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException
     * @see #readAllChars(Reader)
     */
    public static char[] readChars(final Reader source) throws IOException {
        return readChars(source, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static char[] readChars(final Reader source, final long offset, final int maxLen) throws IOException {
        return readChars(source, offset, (long) maxLen);
    }

    private static char[] readChars(final Reader source, final long offset, final long maxLen) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        if ((maxLen == 0) || ((offset > 0) && (skip(source, offset) < offset))) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final char[] buf = Objectory.createCharArrayBuffer();
        char[] charArray = buf;
        int arrayLength = charArray.length;

        int count = 0;
        int cnt = 0;

        try {
            while (count < maxLen && EOF != (cnt = read(source, charArray, count, (int) Math.min(maxLen - count, arrayLength - count)))) { // NOSONAR
                count += cnt;

                if (count < maxLen && count >= arrayLength) {
                    int newCapacity = (int) (arrayLength * 1.75);

                    if (newCapacity < 0 || newCapacity > maxLen || newCapacity > N.MAX_ARRAY_SIZE) {
                        newCapacity = (int) N.min(maxLen, N.MAX_ARRAY_SIZE);
                    }

                    if (newCapacity <= arrayLength) {
                        throw new OutOfMemoryError("Required array size too large");
                    }

                    charArray = Arrays.copyOf(charArray, newCapacity);
                    arrayLength = charArray.length;
                }
            }

            return (count <= 0 ? N.EMPTY_CHAR_ARRAY : N.copyOfRange(charArray, 0, count));
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final File source) throws UncheckedIOException {
        return readAllToString(source, DEFAULT_CHARSET);
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final File source, final Charset encoding) throws UncheckedIOException {
        final byte[] bytes = readAllBytes(source);

        return new String(bytes, checkCharset(encoding));
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final InputStream source) throws UncheckedIOException {
        return readAllToString(source, DEFAULT_CHARSET);
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final InputStream source, final Charset encoding) throws UncheckedIOException {
        final byte[] bytes = readAllBytes(source);

        return new String(bytes, checkCharset(encoding));
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final Reader source) throws UncheckedIOException {
        final char[] chars = readAllChars(source);

        return String.valueOf(chars);
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static String readToString(final File source, final long offset, final int maxLen) throws IOException {
        return readToString(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     *
     * @param source
     * @param encoding
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static String readToString(final File source, final Charset encoding, final long offset, final int maxLen) throws IOException {
        final byte[] bytes = readBytes(source, offset, maxLen);

        return new String(bytes, checkCharset(encoding));
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static String readToString(final InputStream source, final long offset, final int maxLen) throws IOException {
        return readToString(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads a specified number of characters from an InputStream, starting from a specified offset, and converts them into a String.
     * The characters are decoded using the provided Charset.
     *
     * @param source The InputStream to read from.
     * @param encoding The Charset to use for decoding the InputStream.
     * @param offset The position in the InputStream to start reading from.
     * @param maxLen The maximum number of characters to read from the InputStream.
     * @return A String containing the characters read from the InputStream.
     * @throws IOException If an I/O error occurs.
     */
    public static String readToString(final InputStream source, final Charset encoding, final long offset, final int maxLen) throws IOException {
        final byte[] bytes = readBytes(source, offset, maxLen);

        return new String(bytes, checkCharset(encoding));
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    public static String readToString(final Reader source, final long offset, final int maxLen) throws IOException {
        final char[] chs = readChars(source, offset, maxLen);

        return String.valueOf(chs);
    }

    /**
     * Reads all lines from an InputStream and returns them as a List of Strings.
     * This method uses the provided Charset to decode the InputStream.
     * Note: This method should not be used to read InputStreams with line size close to Integer.MAX_VALUE.
     *
     * @param source The InputStream to read from.
     * @param encoding The Charset to use for decoding the InputStream.
     * @return A List of Strings, each string being a line from the InputStream.
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public static List<String> readAllLines(final File source) throws UncheckedIOException {
        return readAllLines(source, DEFAULT_CHARSET);
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with line size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final File source, final Charset encoding) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readAllLines(is, encoding);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with line size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final InputStream source) throws UncheckedIOException {
        return readAllLines(source, DEFAULT_CHARSET);
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with line size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final InputStream source, final Charset encoding) throws UncheckedIOException {

        // try {
        final Reader reader = createReader(source, encoding);
        // } finally {
        // // close(reader);
        // }

        return readAllLines(reader);
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with line size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final Reader source) throws UncheckedIOException {
        final List<String> res = new ArrayList<>();
        final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR

        try {
            String line = null;

            while ((line = br.readLine()) != null) { //NOSONAR
                res.add(line);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != source) {
                Objectory.recycle(br);
            }
        }

        return res;
    }

    /**
     *
     * @param source
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readFirstLine(final File source) throws IOException {
        return readFirstLine(source, DEFAULT_CHARSET);
    }

    /**
     *
     * @param source
     * @param encoding
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readFirstLine(final File source, final Charset encoding) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readFirstLine(is, encoding);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param source
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readFirstLine(final InputStream source) throws IOException {
        return readFirstLine(source, DEFAULT_CHARSET);
    }

    /**
     *
     * @param source
     * @param encoding
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readFirstLine(final InputStream source, final Charset encoding) throws IOException {
        return readFirstLine(createReader(source, encoding));
    }

    /**
     *
     * @param source
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readFirstLine(final Reader source) throws IOException {
        return readLine(source, 0);
    }

    /**
     *
     * @param source
     * @return
     * @throws IOException
     */
    public static String readLastLine(final File source) throws IOException {
        return readLastLine(source, DEFAULT_CHARSET);
    }

    /**
     *
     * @param source
     * @param encoding
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readLastLine(final File source, final Charset encoding) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readLastLine(is, encoding);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param source
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readLastLine(final InputStream source) throws IOException {
        return readLastLine(source, DEFAULT_CHARSET);
    }

    /**
     *
     * @param source
     * @param encoding
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readLastLine(final InputStream source, final Charset encoding) throws IOException {
        return readLastLine(createReader(source, encoding));
    }

    /**
     *
     * @param source
     * @return {@code null} if the specified {@code file/InputStream/Reader} is empty.
     * @throws IOException
     */
    public static String readLastLine(final Reader source) throws IOException {
        final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR

        try {
            String ret = null;
            String line = null;

            while ((line = br.readLine()) != null) { //NOSONAR
                ret = line;
            }

            return ret;
        } finally {
            if (br != source) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     *
     * @param source
     * @param lineIndex
     * @return {@code null} if line size of the specified {@code file/InputStream/Reader} is less than {@code lineIndx + 1}.
     * @throws IOException
     */
    public static String readLine(final File source, final int lineIndex) throws IOException {
        return readLine(source, DEFAULT_CHARSET, lineIndex);
    }

    /**
     *
     * @param source
     * @param encoding
     * @param lineIndex
     * @return {@code null} if line size of the specified {@code file/InputStream/Reader} is less than {@code lineIndx + 1}.
     * @throws IOException
     */
    public static String readLine(final File source, final Charset encoding, final int lineIndex) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readLine(is, encoding, lineIndex);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param source
     * @param lineIndex
     * @return {@code null} if line size of the specified {@code file/InputStream/Reader} is less than {@code lineIndx + 1}.
     * @throws IOException
     */
    public static String readLine(final InputStream source, final int lineIndex) throws IOException {
        return readLine(source, DEFAULT_CHARSET, lineIndex);
    }

    /**
     *
     * @param source
     * @param encoding
     * @param lineIndex
     * @return {@code null} if line size of the specified {@code file/InputStream/Reader} is less than {@code lineIndx + 1}.
     * @throws IOException
     */
    public static String readLine(final InputStream source, final Charset encoding, final int lineIndex) throws IOException {
        return readLine(createReader(source, encoding), lineIndex);
    }

    /**
     *
     * @param source
     * @param lineIndex
     * @return {@code null} if line size of the specified {@code file/InputStream/Reader} is less than {@code lineIndx + 1}.
     * @throws IOException
     */
    public static String readLine(final Reader source, int lineIndex) throws IOException {
        final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR

        try {
            if ((lineIndex != 0)) {
                while (lineIndex-- > 0 && br.readLine() != null) {
                    // continue
                }
            }

            // String result = br.readLine(); //NOSONAR
            //
            // Has trouble for reading first/last line from empty file? // TODO
            //    if (result == null) {
            //        throw new IndexOutOfBoundsException("lineIndex: " + lineIndex + " excceded the total line count of the specified reader/file"); // Should throw IllegalArgumentException
            //    }

            return br.readLine(); //NOSONAR
        } finally {
            if (br != source) {
                Objectory.recycle(br);
            }
        }
    }

    //    /**
    //     * <br />
    //     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
    //     *
    //     * @param source
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     * @deprecated replaced by {@link #readAllLines(File)}
    //     */
    //    @Deprecated
    //    public static List<String> readLines(final File source) throws UncheckedIOException {
    //        return readAllLines(source);
    //    }
    //
    //    /**
    //     * <br />
    //     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
    //     *
    //     * @param source
    //     * @param encoding
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     * @deprecated replaced by {@link #readAllLines(File, Charset)}
    //     */
    //    @Deprecated
    //    public static List<String> readLines(final File source, final Charset encoding) throws UncheckedIOException {
    //        return readAllLines(source, encoding);
    //    }
    //
    //    /**
    //     * <br />
    //     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
    //     *
    //     * @param source
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     * @deprecated replaced by {@link #readAllLines(InputStream)}
    //     */
    //    @Deprecated
    //    public static List<String> readLines(final InputStream source) throws UncheckedIOException {
    //        return readAllLines(source);
    //    }
    //
    //    /**
    //     * <br />
    //     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
    //     *
    //     * @param source
    //     * @param encoding
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     * @deprecated replaced by {@link #readAllLines(InputStream, Charset)}
    //     */
    //    @Deprecated
    //    public static List<String> readLines(final InputStream source, final Charset encoding) throws UncheckedIOException {
    //        return readAllLines(source, encoding);
    //    }
    //
    //    /**
    //     * <br />
    //     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
    //     *
    //     * @param source
    //     * @return
    //     * @throws UncheckedIOException the unchecked IO exception
    //     * @deprecated replaced by {@link #readAllLines(Reader)}
    //     */
    //    @Deprecated
    //    public static List<String> readLines(final Reader source) throws UncheckedIOException {
    //        try {
    //            return readLines(source, 0, Integer.MAX_VALUE);
    //        } catch (IOException e) {
    //            throw new UncheckedIOException(e);
    //        }
    //    }

    /**
     *
     * @param source
     * @param offset line to skip
     * @param count maximum line count to read.
     * @return
     * @throws IOException
     */
    public static List<String> readLines(final File source, final int offset, final int count) throws IOException {
        return readLines(source, DEFAULT_CHARSET, offset, count);
    }

    /**
     *
     * @param source
     * @param encoding
     * @param offset line to skip
     * @param count maximum line count to read.
     * @return
     * @throws IOException
     */
    public static List<String> readLines(final File source, final Charset encoding, final int offset, final int count) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readLines(is, encoding, offset, count);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param source
     * @param offset line to skip
     * @param count maximum line count to read.
     * @return
     * @throws IOException
     */
    public static List<String> readLines(final InputStream source, final int offset, final int count) throws IOException {
        return readLines(source, DEFAULT_CHARSET, offset, count);
    }

    /**
     *
     * @param source
     * @param encoding
     * @param offset line to skip
     * @param count maximum line count to read.
     * @return
     * @throws IOException
     */
    public static List<String> readLines(final InputStream source, final Charset encoding, final int offset, final int count) throws IOException {
        return readLines(createReader(source, encoding), offset, count);
    }

    /**
     *
     * @param source
     * @param offset line to skip
     * @param count maximum line count to read.
     * @return
     * @throws IOException
     */
    public static List<String> readLines(final Reader source, int offset, int count) throws IOException {
        final List<String> res = new ArrayList<>();
        final BufferedReader br = source instanceof BufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR

        try {
            while (offset-- > 0 && br.readLine() != null) { //NOSONAR
                // continue
            }

            String line = null;

            while (count-- > 0 && (line = br.readLine()) != null) { //NOSONAR
                res.add(line);
            }

        } finally {
            if (br != source) {
                Objectory.recycle(br);
            }
        }

        return res;
    }

    /**
     * Reads data from a file into a byte array buffer.
     *
     * @param source The file to read from.
     * @param buf The byte array buffer where the data is to be stored.
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException If an I/O error occurs.
     */
    public static int read(final File source, final byte[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     *
     * @param source
     * @param buf
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len
     * @return
     * @throws IOException
     */
    public static int read(final File source, final byte[] buf, final int off, final int len) throws IOException {
        InputStream is = null;

        try {
            is = IOUtil.newFileInputStream(source);

            return read(is, buf, off, len);
        } finally {
            closeQuietly(is);
        }
    }

    /**
     *
     * @param source
     * @param buf
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static int read(final InputStream source, final byte[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     *
     * @param source
     * @param buf
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static int read(final InputStream source, final byte[] buf, final int off, final int len) throws IOException {
        if ((off < 0) || (off > buf.length) || (len < 0) || ((off + len) > buf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }

        int n = source.read(buf, off, len);

        if (n < 0 || n == len) {
            return n;
        }

        while (n < len) {
            final int n1 = source.read(buf, off + n, len - n);

            if (n1 < 0) {
                break;
            }

            n += n1;
        }

        return n;
    }

    /**
     *
     * @param source
     * @param buf
     * @return
     * @throws IOException
     */
    public static int read(final File source, final char[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     *
     * @param source
     * @param charset
     * @param buf
     * @return
     * @throws IOException
     */
    public static int read(final File source, final Charset charset, final char[] buf) throws IOException {
        return read(source, charset, buf, 0, buf.length);
    }

    /**
     *
     * @param source
     * @param buf
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len
     * @return
     * @throws IOException
     */
    public static int read(final File source, final char[] buf, final int off, final int len) throws IOException {
        return read(source, DEFAULT_CHARSET, buf, off, len);
    }

    /**
     *
     * @param source
     * @param charset
     * @param buf
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    public static int read(final File source, final Charset charset, final char[] buf, final int off, final int len) throws IOException {
        Reader reader = null;

        try { //NOSONAR
            reader = IOUtil.newInputStreamReader(IOUtil.newFileInputStream(source), checkCharset(charset));

            return read(reader, buf, off, len);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     *
     * @param source
     * @param buf
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static int read(final Reader source, final char[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     *
     * @param source
     * @param buf
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static int read(final Reader source, final char[] buf, final int off, final int len) throws IOException {
        if ((off < 0) || (off > buf.length) || (len < 0) || ((off + len) > buf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }

        int n = source.read(buf, off, len);

        if (n < 0 || n == len) {
            return n;
        }

        while (n < len) {
            final int n1 = source.read(buf, off + n, len - n);

            if (n1 < 0) {
                break;
            }

            n += n1;
        }

        return n;
    }

    /**
     * Creates the reader.
     *
     * @param source
     * @param encoding
     * @return
     */
    static InputStreamReader createReader(final InputStream source, final Charset encoding) {
        return encoding == null ? IOUtil.newInputStreamReader(source, DEFAULT_CHARSET) : IOUtil.newInputStreamReader(source, encoding);
    }

    /**
     *
     * @param obj
     * @param output
     * @throws IOException
     */
    public static void writeLine(final Object obj, final File output) throws IOException {
        Writer writer = null;

        try {
            writer = IOUtil.newFileWriter(output);

            writeLine(obj, writer);

            writer.flush();
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param obj
     * @param output
     * @throws IOException
     */
    public static void writeLine(final Object obj, final OutputStream output) throws IOException {
        writeLine(obj, output, false);
    }

    /**
     *
     * @param obj
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void writeLine(final Object obj, final OutputStream output, final boolean flush) throws IOException {
        writeLine(obj, IOUtil.newOutputStreamWriter(output), flush); // NOSONAR
    }

    /**
     *
     * @param obj
     * @param output
     * @throws IOException
     */
    public static void writeLine(final Object obj, final Writer output) throws IOException {
        writeLine(obj, output, false);
    }

    /**
     *
     * @param obj
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void writeLine(final Object obj, final Writer output, final boolean flush) throws IOException {
        if (obj == null) {
            output.write(Strings.NULL_CHAR_ARRAY);
        } else {
            output.write(N.toString(obj));
        }

        output.write(IOUtil.LINE_SEPARATOR);

        if (flush) {
            output.flush();
        }
    }

    /**
     *
     * @param lines
     * @param output
     * @throws IOException
     */
    public static void writeLines(final Iterator<?> lines, final File output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        Writer writer = null;

        try {
            writer = IOUtil.newFileWriter(output);

            writeLines(lines, writer, true);
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param lines
     * @param output
     * @throws IOException
     */
    public static void writeLines(final Iterator<?> lines, final OutputStream output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     *
     * @param lines
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void writeLines(final Iterator<?> lines, final OutputStream output, final boolean flush) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, IOUtil.newOutputStreamWriter(output), flush); // NOSONAR
    }

    /**
     *
     * @param lines
     * @param output
     * @throws IOException
     */
    public static void writeLines(final Iterator<?> lines, final Writer output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     *
     * @param lines
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void writeLines(final Iterator<?> lines, final Writer output, final boolean flush) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        final boolean isBufferedWriter = isBufferedWriter(output);
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR

        try {
            Object line = null;

            while (lines.hasNext()) {
                line = lines.next();

                if (line == null) {
                    bw.write(Strings.NULL_CHAR_ARRAY);
                } else {
                    bw.write(N.toString(line));
                }

                bw.write(IOUtil.LINE_SEPARATOR);
            }

            if (flush || !isBufferedWriter) {
                bw.flush();
            }
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    /**
     *
     * @param lines
     * @param output
     * @throws IOException
     */
    public static void writeLines(final Iterable<?> lines, final File output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        Writer writer = null;

        try {
            writer = IOUtil.newFileWriter(output);

            writeLines(lines, writer, true);
        } finally {
            close(writer);
        }
    }

    //    /**
    //     *
    //     * @param lines
    //     * @param offset
    //     * @param count
    //     * @param output
    //     * @throws IOException
    //     * @see {@link List#subList(int, int)}
    //     * @see {@link N#slice(Collection, int, int)}
    //     * @deprecated {@code offset/count} may be misused with {@code fromIndex/toIndex}. This method will be removed later.
    //     */
    //    @Deprecated
    //    @Beta
    //    public static void writeLines(final Collection<?> lines, final int offset, final int count, final File output) throws IOException {
    //        if (count == 0 && N.size(lines) >= offset) {
    //            return;
    //        }
    //
    //        Writer writer = null;
    //
    //        try {
    //            writer = IOUtil.newFileWriter(output);
    //
    //            writeLines(lines, offset, count, writer, true);
    //        } finally {
    //            close(writer);
    //        }
    //    }

    /**
     *
     * @param lines
     * @param output
     * @throws IOException
     */
    public static void writeLines(final Iterable<?> lines, final OutputStream output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     *
     * @param lines
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void writeLines(final Iterable<?> lines, final OutputStream output, final boolean flush) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, IOUtil.newOutputStreamWriter(output), flush); // NOSONAR
    }

    //    /**
    //     *
    //     * @param lines
    //     * @param offset
    //     * @param count
    //     * @param output
    //     * @see {@link List#subList(int, int)}
    //     * @see {@link N#slice(Collection, int, int)}
    //     * @deprecated {@code offset/count} may be misused with {@code fromIndex/toIndex}. This method will be removed later.
    //     */
    //    @Deprecated
    //    @Beta
    //    public static void writeLines(final Collection<?> lines, final int offset, final int count, final OutputStream output) throws IOException {
    //        if (count == 0 && N.size(lines) >= offset) {
    //            return;
    //        }
    //
    //        writeLines(lines, offset, count, output, false);
    //    }
    //
    //    /**
    //     *
    //     * @param lines
    //     * @param offset
    //     * @param count
    //     * @param output
    //     * @param flush
    //     * @see {@link List#subList(int, int)}
    //     * @see {@link N#slice(Collection, int, int)}
    //     * @deprecated {@code offset/count} may be misused with {@code fromIndex/toIndex}. This method will be removed later.
    //     */
    //    @Deprecated
    //    @Beta
    //    public static void writeLines(final Collection<?> lines, final int offset, final int count, final OutputStream output, final boolean flush)
    //            throws IOException {
    //        if (count == 0 && N.size(lines) >= offset) {
    //            return;
    //        }
    //
    //        writeLines(lines, offset, count, IOUtil.newOutputStreamWriter(output), flush); // NOSONAR
    //    }

    /**
     *
     * @param lines
     * @param output
     * @throws IOException
     */
    public static void writeLines(final Iterable<?> lines, final Writer output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     *
     * @param lines
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void writeLines(final Iterable<?> lines, final Writer output, final boolean flush) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        final boolean isBufferedWriter = isBufferedWriter(output);
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR

        try {
            for (final Object line : lines) {
                if (line == null) {
                    bw.write(Strings.NULL_CHAR_ARRAY);
                } else {
                    bw.write(N.toString(line));
                }

                bw.write(IOUtil.LINE_SEPARATOR);
            }

            if (flush || !isBufferedWriter) {
                bw.flush();
            }
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    //    /**
    //     *
    //     * @param lines
    //     * @param offset
    //     * @param count
    //     * @param output
    //     * @see {@link List#subList(int, int)}
    //     * @see {@link N#slice(Collection, int, int)}
    //     * @deprecated {@code offset/count} may be misused with {@code fromIndex/toIndex}. This method will be removed later.
    //     */
    //    @Deprecated
    //    @Beta
    //    public static void writeLines(final Collection<?> lines, final int offset, final int count, final Writer output) throws IOException {
    //        if (count == 0 && N.size(lines) >= offset) {
    //            return;
    //        }
    //
    //        writeLines(lines, offset, count, output, false);
    //    }
    //
    //    /**
    //     *
    //     * @param lines
    //     * @param offset
    //     * @param count
    //     * @param output
    //     * @param flush
    //     * @see {@link List#subList(int, int)}
    //     * @see {@link N#slice(Collection, int, int)}
    //     * @deprecated {@code offset/count} may be misused with {@code fromIndex/toIndex}. This method will be removed later.
    //     */
    //    @Deprecated
    //    @Beta
    //    public static void writeLines(final Collection<?> lines, final int offset, int count, final Writer output, final boolean flush) throws IOException {
    //        if (count == 0 && N.size(lines) >= offset) {
    //            return;
    //        }
    //
    //        boolean isBufferedWriter = isBufferedWriter(output);
    //        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
    //
    //        try {
    //            int lineNum = 0;
    //
    //            for (Object line : lines) {
    //                if (lineNum++ >= offset) {
    //                    if (line == null) {
    //                        bw.write(Strings.NULL_CHAR_ARRAY);
    //                    } else {
    //                        bw.write(N.toString(line));
    //                    }
    //
    //                    bw.write(IOUtil.LINE_SEPARATOR);
    //
    //                    count--;
    //                }
    //
    //                if (count <= 0) {
    //                    break;
    //                }
    //            }
    //
    //            if (flush || !isBufferedWriter) {
    //                bw.flush();
    //            }
    //        } finally {
    //            if (!isBufferedWriter) {
    //                Objectory.recycle((BufferedWriter) bw);
    //            }
    //        }
    //    }

    /**
     *
     * @param b
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final boolean b, final Writer output) throws IOException {
        output.write(N.stringOf(b));
    }

    //    /**
    //     *
    //     * @param b
    //     * @param output
    //     * @param flush
    //     * @throws IOException Signals that an I/O exception has occurred.
    //     */
    //    static void write(final boolean b, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(b));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     *
     * @param c
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final char c, final Writer output) throws IOException {
        output.write(c);
    }

    //    /**
    //     *
    //     * @param c
    //     * @param output
    //     * @param flush
    //     * @throws IOException Signals that an I/O exception has occurred.
    //     */
    //    static void write(final char c, final Writer output, final boolean flush) throws IOException {
    //        output.write(c);
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     *
     * @param b
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final byte b, final Writer output) throws IOException {
        output.write(N.stringOf(b));
    }

    //    /**
    //     *
    //     * @param b
    //     * @param output
    //     * @param flush
    //     * @throws IOException Signals that an I/O exception has occurred.
    //     */
    //    static void write(final byte b, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(b));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     *
     * @param s
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final short s, final Writer output) throws IOException {
        output.write(N.stringOf(s));
    }

    //    /**
    //     *
    //     * @param s
    //     * @param output
    //     * @param flush
    //     * @throws IOException Signals that an I/O exception has occurred.
    //     */
    //    static void write(final short s, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(s));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     *
     * @param i
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final int i, final Writer output) throws IOException {
        output.write(N.stringOf(i));
    }

    //    /**
    //     *
    //     * @param i
    //     * @param output
    //     * @param flush
    //     * @throws IOException Signals that an I/O exception has occurred.
    //     */
    //    static void write(final int i, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(i));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     *
     * @param lng
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final long lng, final Writer output) throws IOException {
        output.write(N.stringOf(lng));
    }

    //    /**
    //     *
    //     * @param lng
    //     * @param output
    //     * @param flush
    //     * @throws IOException Signals that an I/O exception has occurred.
    //     */
    //    static void write(final long lng, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(lng));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     *
     * @param f
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final float f, final Writer output) throws IOException {
        output.write(N.stringOf(f));
    }

    //    /**
    //     *
    //     * @param f
    //     * @param output
    //     * @param flush
    //     * @throws IOException Signals that an I/O exception has occurred.
    //     */
    //    static void write(final float f, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(f));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     *
     * @param d
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final double d, final Writer output) throws IOException {
        output.write(N.stringOf(d));
    }

    //    /**
    //     *
    //     * @param d
    //     * @param output
    //     * @param flush
    //     * @throws IOException Signals that an I/O exception has occurred.
    //     */
    //    static void write(final double d, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(d));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     *
     * @param obj
     * @param output
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Object obj, final Writer output) throws IOException { // Note: DO NOT remove/update this method because it also protect write(boolean/char/byte/.../double, Writer) from NullPointerException.
        output.write(N.toString(obj));
    }

    /**
     *
     * @param cs
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence cs, final File output) throws IOException {
        write(cs, DEFAULT_CHARSET, output);
    }

    /**
     *
     * @param cs
     * @param charset
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence cs, Charset charset, final File output) throws IOException {
        charset = checkCharset(charset);

        write(toByteArray(cs, charset), output);
    }

    /**
     *
     * @param cs
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence cs, final OutputStream output) throws IOException {
        write(cs, output, false);
    }

    /**
     *
     * @param cs
     * @param charset
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence cs, final Charset charset, final OutputStream output) throws IOException {
        write(cs, charset, output, false);
    }

    /**
     *
     * @param cs
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final CharSequence cs, final OutputStream output, final boolean flush) throws IOException {
        write(cs, DEFAULT_CHARSET, output, flush);
    }

    /**
     *
     * @param cs
     * @param charset
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final CharSequence cs, Charset charset, final OutputStream output, final boolean flush) throws IOException {
        charset = checkCharset(charset);

        output.write(N.toString(cs).getBytes(charset));

        if (flush) {
            output.flush();
        }
    }

    /**
     *
     * @param cs
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence cs, final Writer output) throws IOException {
        write(cs, output, false);
    }

    /**
     *
     * @param cs
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final CharSequence cs, final Writer output, final boolean flush) throws IOException {
        output.append(cs);

        if (flush) {
            output.flush();
        }
    }

    /**
     *
     * @param chars
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final File output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     *
     * @param chars
     * @param charset
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final Charset charset, final File output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, charset, output);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final int offset, final int count, final File output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param charset
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final File output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars2Bytes(chars, offset, count, charset), output);
    }

    /**
     *
     * @param chars
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final OutputStream output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     *
     * @param chars
     * @param charset
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final Charset charset, final OutputStream output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, charset, output);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final int offset, final int count, final OutputStream output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param charset
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final OutputStream output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, charset, output, false);
    }

    /**
     *
     * @param chars
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final char[] chars, final OutputStream output, final boolean flush) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output, flush);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final char[] chars, final int offset, final int count, final OutputStream output, final boolean flush) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output, flush);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param charset
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final OutputStream output, final boolean flush)
            throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars2Bytes(chars, offset, count, charset), output, flush);
    }

    /**
     *
     * @param chars
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final Writer output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param output
     * @throws IOException
     */
    public static void write(final char[] chars, final int offset, final int count, final Writer output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, output, false);
    }

    /**
     *
     * @param chars
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final char[] chars, final Writer output, final boolean flush) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output, flush);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final char[] chars, final int offset, final int count, final Writer output, final boolean flush) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        output.write(chars, offset, count);

        if (flush) {
            output.flush();
        }
    }

    /**
     *
     * @param bytes
     * @param output
     * @throws IOException
     */
    public static void write(final byte[] bytes, final File output) throws IOException {
        if (N.isEmpty(bytes)) {
            return;
        }

        write(bytes, 0, bytes.length, output);
    }

    /**
     *
     *
     * @param bytes
     * @param offset
     * @param count
     * @param output
     * @throws IOException
     */
    public static void write(final byte[] bytes, final int offset, final int count, final File output) throws IOException {
        if (count == 0 && N.len(bytes) >= offset) {
            return;
        }

        OutputStream os = null;

        try {
            createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            write(bytes, offset, count, os);

            os.flush();
        } finally {
            close(os);
        }
    }

    /**
     * @param bytes
     * @param output
     * @throws IOException
     */
    public static void write(final byte[] bytes, final OutputStream output) throws IOException {
        if (N.isEmpty(bytes)) {
            return;
        }

        write(bytes, 0, bytes.length, output);
    }

    /**
     *
     *
     * @param bytes
     * @param offset
     * @param count
     * @param output
     * @throws IOException
     */
    public static void write(final byte[] bytes, final int offset, final int count, final OutputStream output) throws IOException {
        if (count == 0 && N.len(bytes) >= offset) {
            return;
        }

        write(bytes, offset, count, output, false);
    }

    /**
     *
     * @param bytes
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final byte[] bytes, final OutputStream output, final boolean flush) throws IOException {
        if (N.isEmpty(bytes)) {
            return;
        }

        write(bytes, 0, bytes.length, output, flush);
    }

    /**
     *
     *
     * @param bytes
     * @param offset
     * @param count
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final byte[] bytes, final int offset, final int count, final OutputStream output, final boolean flush) throws IOException {
        if (count == 0 && N.len(bytes) >= offset) {
            return;
        }

        output.write(bytes, offset, count);

        if (flush) {
            output.flush();
        }

    }

    /**
     * Writes the content of the source file to the output file.
     *
     * @param source The file to read from.
     * @param output The file to write to.
     * @return The total number of bytes written to the output file.
     * @throws IOException If an I/O error occurs.
     */
    public static long write(final File source, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output);
    }

    /**
     *
     *
     * @param source
     * @param offset
     * @param count
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final File source, final long offset, final long count, final File output) throws IOException {
        OutputStream os = null;
        InputStream is = null;

        try {
            os = IOUtil.newFileOutputStream(output);
            is = IOUtil.newFileInputStream(source);

            return write(is, offset, count, os, true);
        } finally {
            closeQuietly(os);
            closeQuietly(is);
        }
    }

    /**
     *
     * @param source
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final File source, final OutputStream output) throws IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of the source file to the output stream, starting from the specified offset and writing up to the specified count.
     *
     * @param source The file to read from.
     * @param offset The position in the file to start reading from.
     * @param count The maximum number of bytes to write to the output.
     * @param output The output stream to write to.
     * @return The total number of bytes written to the output stream.
     * @throws IOException If an I/O error occurs.
     */
    public static long write(final File source, final long offset, final long count, final OutputStream output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     *
     * @param source
     * @param output
     * @param flush
     * @return
     * @throws IOException
     */
    public static long write(final File source, final OutputStream output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes the content of the source file to the output stream, starting from the specified offset and writing up to the specified count.
     * If the flush parameter is true, the output stream is flushed after the write operation.
     *
     * @param source The file to read from.
     * @param offset The position in the file to start reading from.
     * @param count The maximum number of bytes to write to the output.
     * @param output The output stream to write to.
     * @param flush If true, the output stream is flushed after the write operation.
     * @return The total number of bytes written to the output stream.
     * @throws IOException If an I/O error occurs.
     */
    public static long write(final File source, final long offset, final long count, final OutputStream output, final boolean flush) throws IOException {
        InputStream is = null;
        try {
            is = IOUtil.newFileInputStream(source);

            return write(is, offset, count, output, flush);
        } finally {
            closeQuietly(is);
        }
    }

    //    /**
    //     *
    //     * @param source
    //     * @param output
    //     * @return
    //     * @throws IOException
    //     */
    //    public static long write(final File source, final Writer output) throws IOException {
    //        return write(source, output, false);
    //    }
    //
    //    /**
    //     *
    //     * @param source
    //     * @param offset
    //     * @param count
    //     * @param output
    //     * @return
    //     * @throws IOException
    //     * @deprecated inconsistent with methods: {@code write(File, long, long, File)} and {@code write(File, long, long, OutputStream)}. Unit for offset/count is char here. but it's byte in {@code write(File, long, long, File)} and {@code write(File, long, long, OutputStream)}.
    //     */
    //    @Deprecated
    //    public static long write(final File source, final long offset, final long count, final Writer output) throws IOException {
    //        return write(source, offset, count, output, false);
    //    }
    //
    //    /**
    //     *
    //     * @param source
    //     * @param output
    //     * @param flush
    //     * @return
    //     * @throws IOException
    //     */
    //    public static long write(final File source, final Writer output, final boolean flush) throws IOException {
    //        return write(source, 0, Long.MAX_VALUE, output, flush);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param source
    //     * @param offset by char
    //     * @param count
    //     * @param output
    //     * @param flush
    //     * @return
    //     * @throws IOException
    //     * @deprecated inconsistent with methods: {@code write(File, long, long, File)} and {@code write(File, long, long, OutputStream)}. Unit for offset/count is char here. but it's byte in {@code write(File, long, long, File)} and {@code write(File, long, long, OutputStream)}.
    //     */
    //    @Deprecated
    //    public static long write(final File source, final long offset, final long count, final Writer output, final boolean flush) throws IOException {
    //        Reader reader = null;
    //        try {
    //            reader = IOUtil.newBufferedReader(source);
    //
    //            return write(reader, offset, count, output, flush);
    //        } finally {
    //            closeQuietly(reader);
    //        }
    //    }

    /**
     * Writes the content of the input stream to the specified output file.
     *
     * @param source The input stream to read from.
     * @param output The file to write to.
     * @return The total number of bytes written to the output file.
     * @throws IOException If an I/O error occurs.
     */
    public static long write(final InputStream source, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output);
    }

    /**
     *
     *
     * @param source
     * @param offset by byte
     * @param count
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final InputStream source, final long offset, final long count, final File output) throws IOException {
        OutputStream os = null;

        try {
            createNewFileIfNotExists(output);

            os = IOUtil.newFileOutputStream(output);

            final long result = write(source, offset, count, os);

            os.flush();

            return result;
        } finally {
            close(os);
        }
    }

    /**
     *
     * @param source
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final InputStream source, final OutputStream output) throws IOException {
        return write(source, output, false);
    }

    /**
     *
     *
     * @param source
     * @param offset
     * @param count
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     *
     * @param source
     * @param output
     * @param flush
     * @return
     * @throws IOException
     */
    public static long write(final InputStream source, final OutputStream output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes the content of the input stream to the output stream, starting from the specified offset and writing up to the specified count.
     * If the flush parameter is true, the output stream is flushed after the write operation.
     *
     * @param source The input stream to read from.
     * @param offset The position in the input stream to start reading from.
     * @param count The maximum number of bytes to write to the output.
     * @param output The output stream to write to.
     * @param flush If true, the output stream is flushed after the write operation.
     * @return The total number of bytes written to the output stream.
     * @throws IOException If an I/O error occurs.
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);

        N.checkArgNotNegative(offset, "offset"); // NOSONAR
        N.checkArgNotNegative(count, "count"); // NOSONAR

        final byte[] buf = Objectory.createByteArrayBuffer();

        try {
            if (offset > 0) {
                final long skipped = skip(source, offset);

                if (skipped < offset) {
                    return 0;
                }
            }

            if (count == 0) {
                return 0;
            }

            final int bufLength = buf.length;
            long totalCount = 0;
            int cnt = 0;

            while ((totalCount < count) && (EOF != (cnt = read(source, buf, 0, (int) Math.min(count - totalCount, bufLength))))) {
                output.write(buf, 0, cnt);

                totalCount += cnt;
            }

            if (flush) {
                output.flush();
            }

            return totalCount;
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     *
     * @param source
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final Reader source, final File output) throws IOException {
        return write(source, DEFAULT_CHARSET, output);
    }

    /**
     *
     * @param source
     * @param charset
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final Reader source, final Charset charset, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, charset, output);
    }

    /**
     *
     *
     * @param source
     * @param offset
     * @param count
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final Reader source, final long offset, final long count, final File output) throws IOException {
        return write(source, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     *
     *
     * @param source
     * @param offset by char
     * @param count
     * @param charset
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final Reader source, final long offset, final long count, final Charset charset, final File output) throws IOException {
        Writer writer = null;

        try { //NOSONAR
            writer = IOUtil.newOutputStreamWriter(IOUtil.newFileOutputStream(output), checkCharset(charset));

            final long result = write(source, offset, count, writer);

            writer.flush();

            return result;
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param source
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final Reader source, final Writer output) throws IOException {
        return write(source, output, false);
    }

    /**
     *
     *
     * @param source
     * @param offset
     * @param count
     * @param output
     * @return
     * @throws IOException
     */
    public static long write(final Reader source, final long offset, final long count, final Writer output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     *
     * @param source
     * @param output
     * @param flush
     * @return
     * @throws IOException
     */
    public static long write(final Reader source, final Writer output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes a specified number of characters from a Reader to a Writer, starting from a specified offset.
     * The method can also flush the output after writing, based on the provided boolean value.
     *
     * @param source The Reader to read from.
     * @param offset The position in the Reader to start reading from.
     * @param count The number of characters to read from the Reader and write to the Writer.
     * @param output The Writer to write to.
     * @param flush If true, the output Writer is flushed after writing.
     * @return The total number of characters written to the Writer.
     * @throws IllegalArgumentException If the source Reader is null or if the count is negative.
     * @throws IOException If an I/O error occurs.
     */
    public static long write(final Reader source, final long offset, final long count, final Writer output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);

        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        final char[] buf = Objectory.createCharArrayBuffer();

        try {
            if (offset > 0) {
                skipFully(source, offset);
            }

            if (count == 0) {
                return 0;
            }

            final int bufLength = buf.length;
            long totalCount = 0;
            int cnt = 0;

            while ((totalCount < count) && (EOF != (cnt = read(source, buf, 0, (int) Math.min(count - totalCount, bufLength))))) {
                output.write(buf, 0, cnt);

                totalCount += cnt;
            }

            if (flush) {
                output.flush();
            }

            return totalCount;
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Appends the given byte array to the specified file.
     *
     * @param bytes The byte array to append to the file.
     * @param targetFile The file to which the byte array will be appended.
     * @throws IOException If an I/O error occurs.
     */
    public static void append(final byte[] bytes, final File targetFile) throws IOException {
        if (N.isEmpty(bytes)) {
            return;
        }

        append(bytes, 0, bytes.length, targetFile);
    }

    /**
     *
     *
     * @param bytes
     * @param offset
     * @param count
     * @param targetFile
     * @throws IOException
     */
    public static void append(final byte[] bytes, final int offset, final int count, final File targetFile) throws IOException {
        if (count == 0 && N.len(bytes) >= offset) {
            return;
        }

        OutputStream output = null;

        try { //NOSONAR
            createNewFileIfNotExists(targetFile);

            output = new FileOutputStream(targetFile, true);

            write(bytes, offset, count, output, true);
        } finally {
            close(output);
        }
    }

    /**
     *
     * @param chars
     * @param targetFile
     * @throws IOException
     */
    public static void append(final char[] chars, final File targetFile) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        append(chars, 0, chars.length, targetFile);
    }

    /**
     *
     * @param chars
     * @param charset
     * @param targetFile
     * @throws IOException
     */
    public static void append(final char[] chars, final Charset charset, final File targetFile) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        append(chars, 0, chars.length, charset, targetFile);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param targetFile
     * @throws IOException
     */
    public static void append(final char[] chars, final int offset, final int count, final File targetFile) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        append(chars, offset, count, DEFAULT_CHARSET, targetFile);
    }

    /**
     *
     *
     * @param chars
     * @param offset
     * @param count
     * @param charset
     * @param targetFile
     * @throws IOException
     */
    public static void append(final char[] chars, final int offset, final int count, final Charset charset, final File targetFile) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        append(chars2Bytes(chars, offset, count, charset), targetFile);
    }

    /**
     *
     * @param cs
     * @param targetFile
     * @throws IOException
     */
    public static void append(final CharSequence cs, final File targetFile) throws IOException {
        append(cs, DEFAULT_CHARSET, targetFile);
    }

    /**
     *
     * @param cs
     * @param charset
     * @param targetFile
     * @throws IOException
     */
    public static void append(final CharSequence cs, final Charset charset, final File targetFile) throws IOException {
        append(toByteArray(cs, charset), targetFile);
    }

    /**
     *
     * @param source
     * @param targetFile
     * @return
     * @throws IOException
     */
    public static long append(final File source, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, targetFile);
    }

    /**
     *
     *
     * @param source
     * @param offset
     * @param count
     * @param targetFile
     * @return
     * @throws IOException
     */
    public static long append(final File source, final long offset, final long count, final File targetFile) throws IOException {
        OutputStream output = null;
        InputStream is = null;

        try { //NOSONAR
            createNewFileIfNotExists(targetFile);

            is = IOUtil.newFileInputStream(source);

            output = new FileOutputStream(targetFile, true);

            return write(is, offset, count, output, true);
        } finally {
            closeQuietly(output);
            closeQuietly(is);
        }
    }

    /**
     *
     * @param source
     * @param targetFile
     * @return
     * @throws IOException
     */
    public static long append(final InputStream source, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, targetFile);
    }

    /**
     *
     *
     * @param source
     * @param offset by byte
     * @param count
     * @param targetFile
     * @return
     * @throws IOException
     */
    public static long append(final InputStream source, final long offset, final long count, final File targetFile) throws IOException {
        OutputStream output = null;

        try { //NOSONAR
            createNewFileIfNotExists(targetFile);

            output = new FileOutputStream(targetFile, true);

            return write(source, offset, count, output, true);
        } finally {
            close(output);
        }
    }

    /**
     *
     * @param source
     * @param targetFile
     * @return
     * @throws IOException
     */
    public static long append(final Reader source, final File targetFile) throws IOException {
        return append(source, DEFAULT_CHARSET, targetFile);
    }

    /**
     *
     * @param source
     * @param charset
     * @param targetFile
     * @return
     * @throws IOException
     */
    public static long append(final Reader source, final Charset charset, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, charset, targetFile);
    }

    /**
     *
     *
     * @param source
     * @param offset
     * @param count
     * @param targetFile
     * @return
     * @throws IOException
     */
    public static long append(final Reader source, final long offset, final long count, final File targetFile) throws IOException {
        return append(source, offset, count, DEFAULT_CHARSET, targetFile);
    }

    /**
     *
     *
     * @param source
     * @param offset by char
     * @param count
     * @param charset
     * @param targetFile
     * @return
     * @throws IOException
     */
    public static long append(final Reader source, final long offset, final long count, final Charset charset, final File targetFile) throws IOException {
        Writer writer = null;

        try { //NOSONAR
            createNewFileIfNotExists(targetFile);

            writer = IOUtil.newOutputStreamWriter(new FileOutputStream(targetFile, true), checkCharset(charset));

            final long result = write(source, offset, count, writer);

            writer.flush();

            return result;
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param obj
     * @param targetFile
     * @throws IOException
     */
    public static void appendLine(final Object obj, final File targetFile) throws IOException {
        appendLine(obj, DEFAULT_CHARSET, targetFile);
    }

    /**
     *
     * @param obj
     * @param charset
     * @param targetFile
     * @throws IOException
     */
    public static void appendLine(final Object obj, final Charset charset, final File targetFile) throws IOException {
        final String str = N.toString(obj) + IOUtil.LINE_SEPARATOR;

        append(toByteArray(str, charset), targetFile);
    }

    /**
     *
     *
     * @param lines
     * @param targetFile
     * @throws IOException
     */
    public static void appendLines(final Iterable<?> lines, final File targetFile) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        appendLines(lines, DEFAULT_CHARSET, targetFile);
    }

    /**
     *
     *
     * @param lines
     * @param charset
     * @param targetFile
     * @throws IOException
     */
    public static void appendLines(final Iterable<?> lines, final Charset charset, final File targetFile) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        Writer writer = null;

        try { //NOSONAR
            createNewFileIfNotExists(targetFile);

            writer = IOUtil.newOutputStreamWriter(new FileOutputStream(targetFile, true), checkCharset(charset));

            writeLines(lines, writer, true);
        } finally {
            close(writer);
        }
    }

    /**
     * Writes bytes from {@link ReadableByteChannel} to {@link WritableByteChannel}.
     *
     * @param src
     * @param output
     * @return
     * @throws IOException
     */
    public static long transfer(final ReadableByteChannel src, final WritableByteChannel output) throws IOException {
        N.checkArgNotNull(src, cs.ReadableByteChannel);
        N.checkArgNotNull(output, cs.WritableByteChannel);

        return write(Channels.newInputStream(src), Channels.newOutputStream(output), true);
    }

    /**
     * Skips over and discards a specified number of bytes from the input stream.
     *
     * @param input The InputStream from which bytes are to be skipped.
     * @param toSkip The number of bytes to be skipped.
     * @return The actual number of bytes skipped.
     * @throws IOException If an I/O error occurs, including if the InputStream reaches the end before skipping all the bytes.
     */
    public static long skip(final InputStream input, final long toSkip) throws IOException {
        if (toSkip < 0) {
            throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
        } else if (toSkip == 0) {
            return 0;
        }

        final byte[] buf = Objectory.createByteArrayBuffer();
        long remain = toSkip;

        try {
            while (remain > 0) {
                final long n = read(input, buf, 0, (int) Math.min(remain, buf.length));

                if (n < 0) { // EOF

                    break;
                }

                remain -= n;
            }

            return toSkip - remain;
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Skips over and discards a specified number of characters from the input reader.
     *
     * @param input The Reader from which characters are to be skipped.
     * @param toSkip The number of characters to be skipped.
     * @return The actual number of characters skipped.
     * @throws IOException If an I/O error occurs, including if the Reader reaches the end before skipping all the characters.
     */
    public static long skip(final Reader input, final long toSkip) throws IOException {
        if (toSkip < 0) {
            throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
        } else if (toSkip == 0) {
            return 0;
        }

        final char[] buf = Objectory.createCharArrayBuffer();
        long remain = toSkip;

        try {
            while (remain > 0) {
                final long n = read(input, buf, 0, (int) Math.min(remain, buf.length));

                if (n < 0) { // EOF

                    break;
                }

                remain -= n;
            }

            return toSkip - remain;
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Skips over and discards a specified number of bytes from the input stream.
     *
     * @param input The input stream to be skipped.
     * @param toSkip The number of bytes to be skipped.
     * @throws IOException If an I/O error occurs, including if the input stream reaches the end before skipping all the bytes.
     */
    public static void skipFully(final InputStream input, final long toSkip) throws IOException {
        final long skipped = skip(input, toSkip);

        if (skipped != toSkip) {
            throw new IOException("Bytes to skip: " + toSkip + " actual: " + skipped);
        }
    }

    /**
     * Skips over and discards a specified number of characters from the input reader.
     *
     * @param input The Reader from which characters are to be skipped.
     * @param toSkip The number of characters to be skipped.
     * @throws IOException If an I/O error occurs, including if the Reader reaches the end before skipping all the characters.
     */
    public static void skipFully(final Reader input, final long toSkip) throws IOException {
        final long skipped = skip(input, toSkip);

        if (skipped != toSkip) {
            throw new IOException("Chars to skip: " + toSkip + " actual: " + skipped);
        }
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     *
     * @param file
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static MappedByteBuffer map(final File file) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);

        return map(file, MapMode.READ_ONLY);
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     *
     * Fully maps a file in to memory as per
     * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
     * using the requested {@link MapMode}.
     *
     * <p>Files are mapped from offset 0 to its length.
     *
     * <p>This only works for files {@code <= {@link Integer#MAX_VALUE}} bytes.
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file}
     * @return a buffer reflecting {@code file}
     * @throws IllegalArgumentException
     * @throws IOException
     * @see FileChannel#map(MapMode, long, long)
     * @since 2.0
     */
    public static MappedByteBuffer map(final File file, final MapMode mode) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(mode, cs.mode);

        if (!file.exists()) {
            throw new IllegalArgumentException(file.toString() + " is not found");
        }

        return map(file, mode, 0, file.length());
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     *
     * Maps a file in to memory as per
     * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
     * using the requested {@link MapMode}.
     *
     * <p>Files are mapped from offset 0 to {@code size}.
     *
     * <p>If the mode is {@link MapMode#READ_WRITE} and the file does not exist,
     * it will be created with the requested {@code size}. Thus this method is
     * useful for creating memory mapped files which do not yet exist.
     *
     * <p>This only works for files {@code <= {@link Integer#MAX_VALUE}} bytes.
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file}
     * @param offset
     * @param count
     * @return a buffer reflecting {@code file}
     * @throws IllegalArgumentException
     * @throws IOException
     * @see FileChannel#map(MapMode, long, long)
     * @since 2.0
     */
    public static MappedByteBuffer map(final File file, final MapMode mode, final long offset, final long count) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(mode, cs.mode);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        RandomAccessFile raf = null;

        try { //NOSONAR
            raf = new RandomAccessFile(file, mode == MapMode.READ_ONLY ? "r" : "rw");
            return raf.getChannel().map(mode, offset, count);
        } finally {
            IOUtil.closeQuietly(raf);
        }
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     *
     * Returns the lexically cleaned form of the path name, <i>usually</i> (but
     * not always) equivalent to the original. The following heuristics are used:
     *
     * <ul>
     * <li>empty string becomes .
     * <li>. stays as .
     * <li>fold output ./
     * <li>fold output ../ when possible
     * <li>collapse multiple slashes
     * <li>delete trailing slashes (unless the path is just "/")
     * </ul>
     *
     * <p>These heuristics do not always match the behavior of the filesystem. In
     * particular, consider the path {@code a/../b}, which {@code simplifyPath}
     * will change to {@code b}. If {@code a} is a symlink to {@code x}, {@code
     * a/../b} may refer to a sibling of {@code x}, rather than the sibling of
     * {@code a} referred to by {@code b}.
     *
     * @param pathname
     * @return
     * @since 11.0
     */
    public static String simplifyPath(String pathname) {
        if (Strings.isEmpty(pathname)) {
            return ".";
        }

        pathname = pathname.replace('\\', '/');

        // split the path apart
        final String[] components = pathSplitter.splitToArray(pathname);
        final List<String> path = new ArrayList<>();

        // resolve ., .., and //
        for (final String component : components) {
            if (component.length() == 0 || component.equals(".")) {
                continue; //NOSONAR
            } else if (component.equals("..")) {
                if (path.size() > 0 && !path.get(path.size() - 1).equals("..")) {
                    path.remove(path.size() - 1);
                } else {
                    path.add("..");
                }
            } else {
                path.add(component);
            }
        }

        // put it back together
        String result = Strings.join(path, '/');

        if (pathname.charAt(0) == '/') {
            result = "/" + result;
        }

        while (result.startsWith("/../")) {
            result = result.substring(3);
        }

        if (result.equals("/..")) {
            result = "/";
        } else if ("".equals(result)) {
            result = ".";
        }

        return result;
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     *
     * Returns the <a href="http://en.wikipedia.org/wiki/Filename_extension">file
     * extension</a> for the given file name, or the empty string if the file has
     * no extension.  The result does not include the '{@code .}'.
     *
     * @param fullName
     * @return
     * @throws IllegalArgumentException
     * @since 11.0
     */
    public static String getFileExtension(final String fullName) throws IllegalArgumentException {
        N.checkArgNotNull(fullName);

        final String fileName = new File(fullName).getName();
        final int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     *
     * Returns the file name without its
     * <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
     * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
     *
     * @param file The name of the file to trim the extension from. This can be either a fully
     *     qualified file name (including a path) or just a file name.
     * @return The file name without its path or extension.
     * @throws IllegalArgumentException
     * @since 14.0
     */
    public static String getNameWithoutExtension(final String file) throws IllegalArgumentException {
        N.checkArgNotNull(file);

        final String fileName = new File(file).getName();
        final int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    /**
     *
     * @param appendable
     * @return
     */
    public static AppendableWriter newAppendableWriter(final Appendable appendable) {
        return new AppendableWriter(appendable);
    }

    /**
     *
     * @return
     */
    public static StringWriter newStringWriter() {
        return new StringWriter();
    }

    /**
     *
     * @param initialSize
     * @return
     */
    public static StringWriter newStringWriter(final int initialSize) {
        return new StringWriter(initialSize);
    }

    /**
     *
     * @param sb
     * @return
     */
    public static StringWriter newStringWriter(final StringBuilder sb) {
        return new StringWriter(sb);
    }

    /**
     *
     * @return
     */
    public static ByteArrayOutputStream newByteArrayOutputStream() {
        return new ByteArrayOutputStream();
    }

    /**
     *
     * @param initCapacity
     * @return
     */
    public static ByteArrayOutputStream newByteArrayOutputStream(final int initCapacity) {
        return new ByteArrayOutputStream(initCapacity);
    }

    /**
     *
     *
     * @param file
     * @return
     * @throws UncheckedIOException
     */
    public static FileInputStream newFileInputStream(final File file) throws UncheckedIOException {
        try {
            return new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @param name
     * @return
     * @throws UncheckedIOException
     */
    public static FileInputStream newFileInputStream(final String name) throws UncheckedIOException {
        try {
            return new FileInputStream(name);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @param file
     * @return
     * @throws UncheckedIOException
     */
    public static FileOutputStream newFileOutputStream(final File file) throws UncheckedIOException {
        try {
            return new FileOutputStream(file);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @param name
     * @return
     * @throws UncheckedIOException
     */
    public static FileOutputStream newFileOutputStream(final String name) throws UncheckedIOException {
        try {
            return new FileOutputStream(name);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @param file
     * @return
     * @throws UncheckedIOException
     */
    public static FileReader newFileReader(final File file) throws UncheckedIOException {
        try {
            return new FileReader(file, DEFAULT_CHARSET); // NOSONAR
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @param file
     * @param charset
     * @return
     * @throws UncheckedIOException
     */
    public static FileReader newFileReader(final File file, final Charset charset) throws UncheckedIOException {
        try {
            return new FileReader(file, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @param file
     * @return
     * @throws UncheckedIOException
     */
    public static FileWriter newFileWriter(final File file) throws UncheckedIOException {
        try {
            return new FileWriter(file, DEFAULT_CHARSET); // NOSONAR
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @param file
     * @param charset
     * @return
     * @throws UncheckedIOException
     */
    public static FileWriter newFileWriter(final File file, final Charset charset) throws UncheckedIOException {
        try {
            return new FileWriter(file, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param is
     * @return
     * @throws UncheckedIOException
     */
    public static InputStreamReader newInputStreamReader(final InputStream is) throws UncheckedIOException {
        return new InputStreamReader(is, DEFAULT_CHARSET); // NOSONAR
    }

    /**
     *
     * @param is
     * @param charset
     * @return
     * @throws UncheckedIOException
     */
    public static InputStreamReader newInputStreamReader(final InputStream is, final Charset charset) throws UncheckedIOException {
        return new InputStreamReader(is, checkCharset(charset)); // NOSONAR
    }

    /**
     *
     *
     * @param os
     * @return
     * @throws UncheckedIOException
     */
    public static OutputStreamWriter newOutputStreamWriter(final OutputStream os) throws UncheckedIOException {
        return new OutputStreamWriter(os, DEFAULT_CHARSET); // NOSONAR
    }

    /**
     *
     *
     * @param os
     * @param charset
     * @return
     * @throws UncheckedIOException
     */
    public static OutputStreamWriter newOutputStreamWriter(final OutputStream os, final Charset charset) throws UncheckedIOException {
        return new OutputStreamWriter(os, checkCharset(charset));
    }

    /**
     * New buffered reader.
     *
     * @param filePath
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    static java.io.BufferedReader newBufferedReader(final String filePath) throws UncheckedIOException {
        return newBufferedReader(new File(filePath));
    }

    /**
     *
     * @param file
     * @return
     */
    public static BufferedInputStream newBufferedInputStream(final File file) {
        return new BufferedInputStream(newFileInputStream(file));
    }

    /**
     *
     * @param file
     * @param size
     * @return
     */
    public static BufferedInputStream newBufferedInputStream(final File file, final int size) {
        return new BufferedInputStream(newFileInputStream(file), size);
    }

    /**
     *
     * @param file
     * @return
     */
    public static BufferedOutputStream newBufferedOutputStream(final File file) {
        return new BufferedOutputStream(newFileOutputStream(file));
    }

    /**
     *
     * @param file
     * @param size
     * @return
     */
    public static BufferedOutputStream newBufferedOutputStream(final File file, final int size) {
        return new BufferedOutputStream(newFileOutputStream(file), size);
    }

    /**
     * New buffered reader.
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(final File file) throws UncheckedIOException {
        return new java.io.BufferedReader(newFileReader(file));
    }

    /**
     * New buffered reader.
     *
     * @param file
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(final File file, final Charset charset) throws UncheckedIOException {
        return new java.io.BufferedReader(newInputStreamReader(newFileInputStream(file), checkCharset(charset)));
    }

    /**
     * New buffered reader.
     *
     * @param path
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(final Path path) throws UncheckedIOException {
        try {
            return Files.newBufferedReader(path, DEFAULT_CHARSET);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * New buffered reader.
     *
     * @param path
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(final Path path, final Charset charset) throws UncheckedIOException {
        try {
            return Files.newBufferedReader(path, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * New buffered reader.
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(final InputStream is) throws UncheckedIOException {
        return new java.io.BufferedReader(newInputStreamReader(is)); // NOSONAR
    }

    /**
     * New buffered reader.
     *
     * @param is
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(final InputStream is, final Charset charset) throws UncheckedIOException {
        return new java.io.BufferedReader(newInputStreamReader(is, checkCharset(charset)));
    }

    /**
     * New buffered writer.
     *
     * @param filePath
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    static java.io.BufferedWriter newBufferedWriter(final String filePath) throws UncheckedIOException {
        return newBufferedWriter(new File(filePath));
    }

    /**
     * New buffered writer.
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedWriter newBufferedWriter(final File file) throws UncheckedIOException {
        return new java.io.BufferedWriter(newFileWriter(file));
    }

    /**
     * New buffered writer.
     *
     * @param file
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedWriter newBufferedWriter(final File file, final Charset charset) throws UncheckedIOException {
        return new java.io.BufferedWriter(new OutputStreamWriter(newFileOutputStream(file), checkCharset(charset)));
    }

    /**
     * New buffered writer.
     *
     * @param os
     * @return
     */
    public static java.io.BufferedWriter newBufferedWriter(final OutputStream os) {
        return new java.io.BufferedWriter(newOutputStreamWriter(os)); // NOSONAR
    }

    /**
     * New buffered writer.
     *
     * @param os
     * @param charset
     * @return
     */
    public static java.io.BufferedWriter newBufferedWriter(final OutputStream os, final Charset charset) {
        return new java.io.BufferedWriter(newOutputStreamWriter(os, checkCharset(charset)));
    }

    /**
     * New LZ 4 block input stream.
     *
     * @param is
     * @return
     */
    public static LZ4BlockInputStream newLZ4BlockInputStream(final InputStream is) {
        return new LZ4BlockInputStream(is);
    }

    /**
     * New LZ 4 block output stream.
     *
     * @param os
     * @return
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os) {
        return new LZ4BlockOutputStream(os);
    }

    /**
     * Creates a new input stream with the specified buffer size.
     *
     * @param os
     * @param blockSize
     * @return
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os, final int blockSize) {
        return new LZ4BlockOutputStream(os, blockSize);
    }

    /**
     * New snappy input stream.
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static SnappyInputStream newSnappyInputStream(final InputStream is) throws UncheckedIOException {
        try {
            return new SnappyInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * New snappy output stream.
     *
     * @param os
     * @return
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os) {
        return new SnappyOutputStream(os);
    }

    /**
     * Creates a new input stream with the specified buffer size.
     *
     * @param os
     * @param bufferSize
     * @return
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os, final int bufferSize) {
        return new SnappyOutputStream(os, bufferSize);
    }

    /**
     * New GZIP input stream.
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static GZIPInputStream newGZIPInputStream(final InputStream is) throws UncheckedIOException {
        try {
            return new GZIPInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new input stream with the specified buffer size.
     *
     * @param is
     * @param bufferSize
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static GZIPInputStream newGZIPInputStream(final InputStream is, final int bufferSize) throws UncheckedIOException {
        try {
            return new GZIPInputStream(is, bufferSize);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * New GZIP output stream.
     *
     * @param os
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static GZIPOutputStream newGZIPOutputStream(final OutputStream os) throws UncheckedIOException {
        try {
            return new GZIPOutputStream(os);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new input stream with the specified buffer size.
     *
     * @param os
     * @param bufferSize
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static GZIPOutputStream newGZIPOutputStream(final OutputStream os, final int bufferSize) throws UncheckedIOException {
        try {
            return new GZIPOutputStream(os, bufferSize);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * New Zip input stream.
     *
     * @param is
     * @return
     */
    public static ZipInputStream newZipInputStream(final InputStream is) {
        return new ZipInputStream(is);
    }

    /**
     * Creates a new input stream with the specified buffer size.
     *
     * @param is
     * @param charset
     * @return
     */
    public static ZipInputStream newZipInputStream(final InputStream is, final Charset charset) {
        return new ZipInputStream(is, checkCharset(charset));
    }

    /**
     * New Zip output stream.
     *
     * @param os
     * @return
     */
    public static ZipOutputStream newZipOutputStream(final OutputStream os) {
        return new ZipOutputStream(os);
    }

    /**
     * Creates a new input stream with the specified buffer size.
     *
     * @param os
     * @param charset
     * @return
     */
    public static ZipOutputStream newZipOutputStream(final OutputStream os, final Charset charset) {
        return new ZipOutputStream(os, charset);
    }

    /**
     * New GZIP input stream.
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static BrotliInputStream newBrotliInputStream(final InputStream is) throws UncheckedIOException {
        try {
            return new BrotliInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //    public static BrotliOutputStream newBrotliOutputStream(final OutputStream os) throws UncheckedIOException {
    //        try {
    //            return new BrotliOutputStream(os);
    //        } catch (IOException e) {
    //            throw new UncheckedIOException(e);
    //        }
    //    }

    /**
     * Closes a URLConnection.
     *
     * @param conn the connection to close.
     * @since 2.4
     */
    public static void close(final URLConnection conn) {
        if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection) conn).disconnect();
        }
    }

    /**
     * Closes the provided AutoCloseable object.
     * If the object is null, this method does nothing.
     * Any exceptions that occur during the closing operation are caught and not rethrown.
     *
     * @param closeable The AutoCloseable object to be closed. It can be null.
     */
    public static void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e);
            }
        }
    }

    /**
     *
     * @param closeable
     * @param exceptionHandler
     */
    public static void close(final AutoCloseable closeable, final Consumer<Exception> exceptionHandler) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception e) {
                exceptionHandler.accept(e);
            }
        }
    }

    /**
     *
     * @param a
     */
    @SafeVarargs
    public static void closeAll(final AutoCloseable... a) {
        if (N.isEmpty(a)) {
            return;
        }

        closeAll(Arrays.asList(a));
    }

    /**
     *
     * @param c
     */
    public static void closeAll(final Iterable<? extends AutoCloseable> c) {
        if (N.isEmpty(c)) {
            return;
        }

        Exception ex = null;

        for (final AutoCloseable closeable : c) {
            try {
                close(closeable);
            } catch (final Exception e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex.addSuppressed(e);
                }
            }
        }

        if (ex != null) {
            throw ExceptionUtil.toRuntimeException(ex);
        }
    }

    /**
     * Closes the provided AutoCloseable object quietly.
     * Any exceptions that occur during the closing operation are ignored.
     *
     * @param closeable The AutoCloseable object to be closed. It can be null.
     */
    public static void closeQuietly(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception e) {
                // ignore
                logger.error("Failed to close", e);
            }
        }
    }

    /**
     * Close all quietly.
     *
     * @param a
     */
    @SafeVarargs
    public static void closeAllQuietly(final AutoCloseable... a) {
        if (N.isEmpty(a)) {
            return;
        }

        closeAllQuietly(Arrays.asList(a));
    }

    /**
     * Close all quietly.
     *
     * @param c
     */
    public static void closeAllQuietly(final Iterable<? extends AutoCloseable> c) {
        if (N.isEmpty(c)) {
            return;
        }

        for (final AutoCloseable closeable : c) {
            closeQuietly(closeable);
        }
    }

    //    /**
    //     *
    //     * @param srcFile
    //     * @param destDir
    //     * @throws UncheckedIOException the unchecked IO exception
    //     * @deprecated Use {@link #copyToDirectory(File,File)} instead
    //     */
    //    @Deprecated
    //    public static void copyFileToDirectory(final File srcFile, final File destDir) throws UncheckedIOException {
    //        copyToDirectory(srcFile, destDir);
    //    }
    //
    //    /**
    //     *
    //     * @param srcFile
    //     * @param destDir
    //     * @param preserveFileDate
    //     * @throws UncheckedIOException the unchecked IO exception
    //     * @deprecated Use {@link #copyToDirectory(File,File, boolean)} instead
    //     */
    //    @Deprecated
    //    public static void copyFileToDirectory(final File srcFile, final File destDir, final boolean preserveFileDate) throws UncheckedIOException {
    //        copyToDirectory(srcFile, destDir, preserveFileDate);
    //    }
    //
    //    /**
    //     * Copy the specified <code>scrFile</code> if it's a file or its sub files/directories if it's a directory to the target <code>destDir</code> with the specified <code>filter</code>.
    //     *
    //     * @param <E>
    //     * @param srcFile
    //     * @param destDir
    //     * @param preserveFileDate
    //     * @param filter
    //     * @throws UncheckedIOException the unchecked IO exception
    //     * @throws E the e
    //     * @deprecated Use {@link #copyToDirectory(File,File,boolean,Throwables.BiPredicate<? super File, ? super File, E>)} instead
    //     */
    //    @Deprecated
    //    public static <E extends Exception> void copyFileToDirectory(File srcFile, File destDir, final boolean preserveFileDate,
    //            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws UncheckedIOException, E {
    //        copyToDirectory(srcFile, destDir, preserveFileDate, filter);
    //    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     *
     * @param srcFile The source file or directory to be copied. It must not be {@code null}.
     * @param destDir The destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @throws UncheckedIOException If an I/O error occurs during the copying process.
     */
    public static void copyToDirectory(final File srcFile, final File destDir) throws UncheckedIOException {
        copyToDirectory(srcFile, destDir, true);
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     * If the source is a directory, all its contents will be copied into the destination directory.
     *
     * @param srcFile The source file or directory to be copied. It must not be {@code null}.
     * @param destDir The destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @param preserveFileDate If true, the last modified date of the file will be preserved in the copied file.
     * @throws UncheckedIOException If an I/O error occurs during the copying process.
     */
    public static void copyToDirectory(final File srcFile, final File destDir, final boolean preserveFileDate) throws UncheckedIOException {
        copyToDirectory(srcFile, destDir, preserveFileDate, Fn.BiPredicates.alwaysTrue());
    }

    /**
     * Copy the specified <code>scrFile</code> if it's a file or its sub files/directories if it's a directory to the target <code>destDir</code> with the specified <code>filter</code>.
     *
     * @param <E>
     * @param srcFile
     * @param destDir
     * @param preserveFileDate
     * @param filter
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */

    /**
     * Copies the specified source file or directory to the specified destination directory.
     * If the source is a directory, all its contents will be copied into the destination directory.
     *
     * @param <E> The type of the exception that may be thrown by the filter.
     * @param srcFile The source file or directory to be copied. It must not be {@code null}.
     * @param destDir The destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @param preserveFileDate If true, the last modified date of the file will be preserved in the copied file.
     * @param filter A BiPredicate that takes the source and destination files as arguments and returns a boolean. If the predicate returns true, the file is copied; if it returns false, the file is not copied.
     * @throws UncheckedIOException If an I/O error occurs during the copying process.
     * @throws E If the filter throws an exception.
     */
    public static <E extends Exception> void copyToDirectory(File srcFile, File destDir, final boolean preserveFileDate,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws UncheckedIOException, E {
        if (!srcFile.exists()) {
            throw new IllegalArgumentException("The source file doesn't exist: " + srcFile.getAbsolutePath());
        }

        if (destDir.exists()) {
            if (destDir.isFile()) {
                throw new IllegalArgumentException("The destination file must be directory: " + destDir.getAbsolutePath());
            }
        } else {
            if (!destDir.mkdirs()) {
                throw new UncheckedIOException(new IOException("Failed to create destination directory: " + destDir.getAbsolutePath()));
            }
        }

        if (!destDir.canWrite()) {
            throw new UncheckedIOException(new IOException("Destination '" + destDir + "' cannot be written to")); //NOSONAR
        }

        String destCanonicalPath = null;
        String srcCanonicalPath = null;
        try {
            srcFile = srcFile.getCanonicalFile();
            destDir = destDir.getCanonicalFile();
            destCanonicalPath = destDir.getCanonicalPath();
            srcCanonicalPath = srcFile.getCanonicalPath();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        if (srcFile.isDirectory()) {
            if (destCanonicalPath.startsWith(srcCanonicalPath) && (destCanonicalPath.length() == srcCanonicalPath.length()
                    || destCanonicalPath.charAt(srcCanonicalPath.length()) == '/' || destCanonicalPath.charAt(srcCanonicalPath.length()) == '\\')) {
                throw new IllegalArgumentException(
                        "Failed to copy due to the target directory: " + destCanonicalPath + " is in or same as the source directory: " + srcCanonicalPath);
            }

            try {
                doCopyDirectory(srcFile, destDir, preserveFileDate, filter);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            File destFile = null;

            try {
                if (destDir.getCanonicalPath().equals(srcFile.getParentFile().getCanonicalPath())) {
                    destFile = new File(destDir, "Copy of " + srcFile.getName());
                } else {
                    destFile = new File(destDir, srcFile.getName());
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            doCopyFile(srcFile, destFile, preserveFileDate);
        }
    }

    /**
     * Internal copy directory method.
     *
     * @param <E>
     * @param srcDir the validated source directory, must not be {@code null}
     * @param destDir the validated destination directory, must not be {@code null}
     * @param preserveFileDate whether to preserve the file date
     * @param filter the filter to apply, null means copy all directories and files
     * @throws IOException             if an error occurs
     * @throws E the e
     * @since 1.1
     */
    private static <E extends Exception> void doCopyDirectory(final File srcDir, final File destDir, final boolean preserveFileDate,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws IOException, E {
        if (destDir.exists()) {
            if (destDir.isFile()) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (!destDir.mkdirs()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        }

        final File[] subFiles = srcDir.listFiles();

        if (N.isEmpty(subFiles)) {
            return;
        }

        for (final File subFile : subFiles) {
            if (subFile == null) {
                continue;
            }

            if (filter == null || filter.test(srcDir, subFile)) {
                final File dest = new File(destDir, subFile.getName());

                if (subFile.isDirectory()) {
                    doCopyDirectory(subFile, dest, preserveFileDate, Fn.BiPredicates.alwaysTrue());
                } else {
                    doCopyFile(subFile, dest, preserveFileDate);
                }
            } else if (subFile.isDirectory()) {
                final File dest = new File(destDir, subFile.getName());
                doCopyDirectory(subFile, dest, preserveFileDate, filter);
            }
        }

        // Do this last, as the above has probably affected directory metadata
        if (preserveFileDate) {
            destDir.setLastModified(srcDir.lastModified()); //NOSONAR
        }
    }

    /**
     * Internal copy file method.
     *
     * @param srcFile
     *            the validated source file, must not be {@code null}
     * @param destFile
     *            the validated destination file, must not be {@code null}
     * @param preserveFileDate
     *            whether to preserve the file date
     */
    private static void doCopyFile(final File srcFile, final File destFile, final boolean preserveFileDate) {
        if (destFile.exists()) {
            throw new IllegalArgumentException("The destination file already existed: " + destFile.getAbsolutePath());
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;

        try {
            fis = IOUtil.newFileInputStream(srcFile);
            fos = IOUtil.newFileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();

            final long size = input.size();
            long pos = 0;
            long count = 0;

            while (pos < size) {
                count = ((size - pos) > FILE_COPY_BUFFER_SIZE) ? FILE_COPY_BUFFER_SIZE : (size - pos);
                pos += output.transferFrom(input, pos, count);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(output);
            close(fos);
            close(input);
            close(fis);
        }

        if (srcFile.length() != destFile.length()) {
            deleteAllIfExists(destFile);
            throw new UncheckedIOException(new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'"));
        }

        if (preserveFileDate) {
            destFile.setLastModified(srcFile.lastModified()); //NOSONAR
        }
    }

    /**
     *
     * @param source
     * @param target
     * @param options
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @see {@link Files#copy(Path, Path, CopyOption...)}
     */
    @SafeVarargs
    public static Path copy(final Path source, final Path target, final CopyOption... options) throws UncheckedIOException {
        try {
            return Files.copy(source, target, options);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param in
     * @param target
     * @param options
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @see Files#copy(InputStream, Path, CopyOption...)
     */
    @SafeVarargs
    public static long copy(final InputStream in, final Path target, final CopyOption... options) throws UncheckedIOException {
        try {
            return Files.copy(in, target, options);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param source
     * @param output
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @see Files#copy(Path, OutputStream)
     */
    public static long copy(final Path source, final OutputStream output) throws UncheckedIOException {
        try {
            return Files.copy(source, output);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Copies bytes from the URL <code>source</code> to a file
     * <code>destination</code>. The directories up to <code>destination</code>
     * will be created if they don't already exist. <code>destination</code>
     * will be overwritten if it already exists.
     * <p>
     * Warning: this method does not set a connection or read timeout and thus
     * might block forever. Use {@link #copyURLToFile(URL, File, int, int)}
     * with reasonable timeouts to prevent this.
     *
     * @param source the <code>URL</code> to copy bytes from, must not be {@code null}
     * @param destination the non-directory <code>File</code> to write bytes to
     *  (possibly overwriting), must not be {@code null}
     * @throws UncheckedIOException if an IO error occurs during copying
     */
    public static void copyURLToFile(final URL source, final File destination) throws UncheckedIOException {
        InputStream is = null;
        try {
            is = source.openStream();

            write(is, destination);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
        }
    }

    /**
     * Copies bytes from the URL <code>source</code> to a file
     * <code>destination</code>. The directories up to <code>destination</code>
     * will be created if they don't already exist. <code>destination</code>
     * will be overwritten if it already exists.
     *
     * @param source the <code>URL</code> to copy bytes from, must not be {@code null}
     * @param destination the non-directory <code>File</code> to write bytes to
     *  (possibly overwriting), must not be {@code null}
     * @param connectionTimeout the number of milliseconds until this method
     *  will timeout if no connection could be established to the <code>source</code>
     * @param readTimeout the number of milliseconds until this method will
     *  timeout if no data could be read from the <code>source</code>
     * @throws UncheckedIOException if an IO error occurs during copying
     */
    public static void copyURLToFile(final URL source, final File destination, final int connectionTimeout, final int readTimeout) throws UncheckedIOException {
        InputStream is = null;
        try {
            final URLConnection connection = source.openConnection();
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            is = connection.getInputStream();

            write(is, destination);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
        }
    }

    /**
     *
     * @param srcFile
     * @param destDir
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void move(final File srcFile, final File destDir) throws UncheckedIOException {
        if (!srcFile.exists()) {
            throw new IllegalArgumentException("The source file doesn't exist: " + srcFile.getAbsolutePath());
        }

        if (destDir.exists()) {
            if (destDir.isFile()) {
                throw new IllegalArgumentException("The destination file must be directory: " + destDir.getAbsolutePath());
            }
        } else {
            if (!destDir.mkdirs()) {
                throw new UncheckedIOException(new IOException("Failed to create destination directory: " + destDir.getAbsolutePath()));
            }
        }

        final File destFile = new File(destDir, srcFile.getName());

        if (!srcFile.renameTo(destFile)) {
            throw new UncheckedIOException(new IOException("Failed to move file from: " + srcFile.getAbsolutePath() + " to: " + destDir.getAbsolutePath()));
        }
    }

    /**
     *
     * @param source
     * @param target
     * @param options
     * @return
     * @throws UncheckedIOException
     * @see {@link Files#move(Path, Path, CopyOption...)}
     */
    public static Path move(final Path source, final Path target, final CopyOption... options) throws UncheckedIOException {
        try {
            return Files.copy(source, target, options);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param srcFile
     * @param newFileName
     * @return <code>true</code> if and only if the renaming succeeded;
     *          <code>false</code> otherwise
     */
    public static boolean renameTo(final File srcFile, final String newFileName) {
        return srcFile.renameTo(new File(srcFile.getParent() + IOUtil.DIR_SEPARATOR + newFileName));
    }

    /**
     * Delete the specified file (or directory).
     *
     * @param file
     * @return true if the file is deleted successfully, otherwise false if the file is null or doesn't exist, or can't be deleted.
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     */
    public static boolean deleteIfExists(final File file) {
        if ((file == null) || !file.exists()) {
            return false;
        }

        return file.delete(); //NOSONAR
    }

    /**
     *
     * @param file
     * @return
     */
    public static boolean deleteQuietly(final File file) {
        try {
            return deleteIfExists(file);
        } catch (final Throwable e) {
            logger.error("Failed to delete file: " + file, e);
            return false;
        }
    }

    /**
     * Delete the specified file and all its sub files/directories if it's a directory.
     *
     * @param file
     * @return true if the file is deleted successfully, otherwise false if the file is null or doesn't exist, or can't be deleted.
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     */
    public static boolean deleteAllIfExists(final File file) {
        if ((file == null) || !file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            final File[] files = file.listFiles();

            if (N.notEmpty(files)) {
                for (final File subFile : files) {
                    if (subFile == null) {
                        continue;
                    }

                    if (subFile.isFile()) {
                        if (!subFile.delete()) { //NOSONAR
                            return false;
                        }
                    } else {
                        if (!deleteAllIfExists(subFile)) {
                            return false;
                        }
                    }
                }
            }
        }

        return file.delete(); //NOSONAR
    }

    /**
     * Delete the specified <code>file</code> if it's a file or its sub files/directories if it's a directory.
     *
     * @param file
     * @return
     */
    public static boolean deleteFiles(final File file) {
        return deleteFiles(file, BiPredicates.alwaysTrue());
    }

    /**
     * Delete the specified <code>file</code> if it's a file or its sub files/directories if it's a directory with the specified filter.
     *
     * @param <E>
     * @param file
     * @param filter
     * @return {@code false} if the file or some of its sub files/directories can't be deleted.
     * @throws E the e
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     */
    public static <E extends Exception> boolean deleteFiles(final File file, final Throwables.BiPredicate<? super File, ? super File, E> filter) throws E {
        if ((file == null) || !file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            final File[] files = file.listFiles();

            if (N.isEmpty(files)) {
                return true;
            }

            for (final File subFile : files) {
                if (subFile == null) {
                    continue;
                }

                if (filter == null || filter.test(file, subFile)) {
                    if (subFile.isFile()) {
                        if (!subFile.delete()) { //NOSONAR
                            return false;
                        }
                    } else {
                        if (!deleteAllIfExists(subFile)) {
                            return false;
                        }
                    }
                } else {
                    if (subFile.isDirectory() && !deleteFiles(subFile, filter)) {
                        return false;
                    }
                }
            }
        } else {
            if (filter == null || filter.test(file.getParentFile(), file)) {
                return file.delete(); //NOSONAR
            }
        }

        return true;
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory directory to clean
     * @return {@code false} if some of its sub files can't be deleted.
     * @see #deleteFiles(File, com.landawn.abacus.util.Throwables.BiPredicate)
     */
    public static boolean cleanDirectory(final File directory) {
        if (directory == null || !directory.isDirectory()) {
            return true;
        }

        return deleteFiles(directory, BiPredicates.alwaysTrue());
    }

    static void createNewFileIfNotExists(final File file) throws IOException {
        if (!file.exists() && (file.createNewFile() == false)) { //NOSONAR
            throw new IOException("Failed to create new file: " + file.getName());
        }
    }

    /**
     * Creates the if not exists.
     *
     * @param file
     * @return <code>false</code> if file exists or failed to create new file.
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static boolean createIfNotExists(final File file) throws UncheckedIOException {
        try {
            return file.exists() ? false : file.createNewFile();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     *
     * @param dir
     * @return
     */
    public static boolean mkdirIfNotExists(final File dir) {
        if (!(dir.exists() && dir.isDirectory())) {
            return dir.mkdir();
        }

        return false;
    }

    /**
     *
     *
     * @param dir
     * @return
     */
    public static boolean mkdirsIfNotExists(final File dir) {
        if (!(dir.exists() && dir.isDirectory())) {
            return dir.mkdirs();
        }

        return false;
    }

    /**
     *
     *
     * @param writer
     * @return
     */
    public static boolean isBufferedWriter(final Writer writer) {
        return writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
    }

    /**
     * Tests if the specified {@code File} is newer than the specified {@code Date}.
     *
     * @param file the {@code File} of which the modification date must be compared
     * @param date the date reference
     * @return true if the {@code File} exists and has been modified after the given {@code Date}.
     * @throws IllegalArgumentException if the file or date is {@code null}
     */
    public static boolean isFileNewer(final File file, final Date date) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(date, cs.date);

        return file.lastModified() > date.getTime();
    }

    /**
     * Tests if the specified {@code File} is newer than the reference {@code File}.
     *
     * @param file the {@code File} of which the modification date must be compared
     * @param reference the {@code File} of which the modification date is used
     * @return true if the {@code File} exists and has been modified more recently than the reference {@code File}
     * @throws IllegalArgumentException if the file or reference file is {@code null} or the reference file doesn't exist
     */
    public static boolean isFileNewer(final File file, final File reference) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(reference, cs.reference);

        return file.lastModified() > reference.lastModified();
    }

    /**
     * Tests if the specified {@code File} is older than the specified {@code Date}.
     *
     * @param file the {@code File} of which the modification date must be compared
     * @param date the date reference
     * @return true if the {@code File} exists and has been modified before the given {@code Date}
     * @throws IllegalArgumentException if the file or date is {@code null}
     */
    public static boolean isFileOlder(final File file, final Date date) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(date, cs.date);

        return file.lastModified() < date.getTime();
    }

    /**
     * Tests if the specified {@code File} is older than the reference {@code File}.
     *
     * @param file the {@code File} of which the modification date must be compared
     * @param reference the {@code File} of which the modification date is used
     * @return true if the {@code File} exists and has been modified before the reference {@code File}
     * @throws IllegalArgumentException if the file or reference file is {@code null} or the reference file doesn't exist
     */
    public static boolean isFileOlder(final File file, final File reference) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(reference, cs.reference);

        return file.lastModified() < reference.lastModified();
    }

    /**
     * Determines whether the specified file is a Symbolic Link rather than an actual file.
     * <p>
     * Will not return true if there is a Symbolic Link anywhere in the path,
     * only if the specific file is.
     * </p>
     * <p>
     * When using jdk1.7, this method delegates to {@code boolean java.nio.file.Files.isSymbolicLink(Path path)}
     * </p>
     *
     * <p>
     * <b>Note:</b> the current implementation always returns {@code false} if running on
     * jkd1.6 and the system is detected as Windows using {@link FilenameUtil#isSystemWindows()}
     * </p>
     * <p>
     * For code that runs on Java 1.7 or later, use the following method instead:
     * </p>
     *
     * {@code boolean java.nio.file.Files.isSymbolicLink(Path path)}
     *
     * @param file the file to check
     * @return true if the file is a Symbolic Link
     * @throws IllegalArgumentException
     * @since 2.0
     */
    public static boolean isSymbolicLink(final File file) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);

        return Files.isSymbolicLink(file.toPath());
    }

    /**
     * Returns the size of the specified file or directory. If the provided
     * {@link File} is a regular file, then the file's length is returned.
     * If the argument is a directory, then the size of the directory is
     * calculated recursively. If a directory or subdirectory is security
     * restricted, its size will not be included.
     * <p>
     * Note that overflow is not detected, and the return value may be negative if
     * overflow occurs. See {@link #sizeOfAsBigInteger(File)} for an alternative
     * method that does not overflow.
     * </p>
     *
     * @param file the regular file or directory to return the size
     *             of (must not be {@code null}).
     *
     * @return the length of the file, or recursive size of the directory, provided (in bytes).
     *
     * @throws IllegalArgumentException  if the file is {@code null} or the file does not exist.
     *
     * @since 2.0
     */
    public static long sizeOf(final File file) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);

        if (!file.exists()) {
            final String message = file + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (file.isDirectory()) {
            return sizeOfDirectory0(file); // private method; expects directory
        }

        return file.length();
    }

    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     * <p>
     * Note that overflow is not detected, and the return value may be negative if
     * overflow occurs. See {@link #sizeOfDirectoryAsBigInteger(File)} for an alternative
     * method that does not overflow.
     * </p>
     *
     * @param directory directory to inspect, must not be {@code null}
     * @return size of directory in bytes, 0 if directory is security restricted, a negative number when the real total is greater than {@link Long#MAX_VALUE}.
     * @throws IllegalArgumentException if the directory is {@code null} or it's not existed directory.
     */
    public static long sizeOfDirectory(final File directory) throws IllegalArgumentException {
        N.checkArgNotNull(directory, cs.directory);

        checkDirectory(directory);

        return sizeOfDirectory0(directory);
    }

    /**
     * the size of a file
     * @param file the file to check
     * @return the size of the file
     */
    private static long sizeOf0(final File file) {
        if (file.isDirectory()) {
            return sizeOfDirectory0(file);
        }

        return file.length(); // will be 0 if file does not exist
    }

    /**
     * the size of a director
     * @param directory the directory to check
     * @return the size
     */
    private static long sizeOfDirectory0(final File directory) {
        final File[] files = directory.listFiles();

        if (files == null) { // null if security restricted
            return 0L;
        }

        long size = 0;

        for (final File file : files) {
            if (!isSymbolicLink(file)) {
                size += sizeOf0(file); // internal method
                if (size < 0) {
                    break;
                }
            }
        }

        return size;
    }

    /**
     * Checks that the given {@code File} exists and is a directory.
     *
     * @param directory The {@code File} to check.
     * @throws IllegalArgumentException if the given {@code File} does not exist or is not a directory.
     */
    private static void checkDirectory(final File directory) {
        if (!directory.exists()) {
            throw new IllegalArgumentException(directory + " does not exist");
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
    }

    /**
     *
     * @param sourceFile
     * @param targetFile
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void zip(final File sourceFile, final File targetFile) throws UncheckedIOException {
        ZipOutputStream zos = null;

        try { //NOSONAR
            zos = new ZipOutputStream(IOUtil.newFileOutputStream(targetFile));
            zipFile(sourceFile, zos, targetFile);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(zos);
        }
    }

    /**
     *
     * @param sourceFiles
     * @param targetFile
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void zip(final Collection<File> sourceFiles, final File targetFile) throws UncheckedIOException {
        ZipOutputStream zos = null;

        try { //NOSONAR
            zos = new ZipOutputStream(IOUtil.newFileOutputStream(targetFile));

            for (final File sourceFile : sourceFiles) {
                zipFile(sourceFile, zos, targetFile);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(zos);
        }
    }

    /**
     *
     * @param sourceFile
     * @param zos
     * @param targetFile
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void zipFile(final File sourceFile, final ZipOutputStream zos, final File targetFile) throws IOException {
        if (sourceFile.isFile()) {
            zipFile(sourceFile, null, zos, targetFile);
        } else {
            final List<File> subFileList = listFiles(sourceFile, true, true);

            // subFileList.add(sourceFile);
            for (final File subFile : subFileList) {
                zipFile(subFile, sourceFile, zos, targetFile);
            }
        }
    }

    /**
     *
     * @param file
     * @param sourceDir
     * @param zos
     * @param targetFile
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void zipFile(final File file, final File sourceDir, final ZipOutputStream zos, final File targetFile) throws IOException {
        if (file.equals(targetFile)) {
            return;
        }

        String relativeFileName = null;

        if (sourceDir == null) {
            relativeFileName = file.getName();
        } else {
            relativeFileName = getRelativePath(sourceDir, file);
        }

        final ZipEntry ze = new ZipEntry(relativeFileName);
        ze.setSize(file.length());
        ze.setTime(file.lastModified());
        zos.putNextEntry(ze);

        final InputStream is = IOUtil.newFileInputStream(file);

        final byte[] buf = Objectory.createByteArrayBuffer();

        try {
            int count = 0;

            while (EOF != (count = read(is, buf, 0, buf.length))) {
                zos.write(buf, 0, count);
            }
        } finally {
            Objectory.recycle(buf);

            closeQuietly(is);
        }
    }

    /**
     *
     * @param srcZipFile
     * @param targetDir
     */
    public static void unzip(final File srcZipFile, final File targetDir) {
        ZipFile zip = null;
        ZipEntry ze = null;
        OutputStream os = null;
        InputStream is = null;

        final byte[] buf = Objectory.createByteArrayBuffer();
        final int bufLength = buf.length;

        try { //NOSONAR
            zip = new ZipFile(srcZipFile);

            final Enumeration<? extends ZipEntry> entryEnum = zip.entries();

            while (entryEnum.hasMoreElements()) {
                ze = entryEnum.nextElement();

                if (ze.isDirectory()) {
                    continue;
                }

                os = IOUtil.newFileOutputStream(getAbsolutePath(targetDir, ze.getName()));

                is = zip.getInputStream(ze);

                int count = 0;

                while (EOF != (count = read(is, buf, 0, bufLength))) {
                    os.write(buf, 0, count);
                }

                os.flush();

                closeQuietly(is);
                is = null;
                close(os);
                os = null;
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);

            closeQuietly(zip);
            closeQuietly(is);
            close(os);
        }
    }

    /**
     *
     * @param file
     * @param countOfParts
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void split(final File file, final int countOfParts) throws UncheckedIOException {
        split(file, countOfParts, file.getParentFile());
    }

    /**
     *
     * @param file
     * @param countOfParts
     * @param destDir
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void split(final File file, final int countOfParts, final File destDir) throws UncheckedIOException {
        final long sizeOfPart = (file.length() % countOfParts) == 0 ? (file.length() / countOfParts) : (file.length() / countOfParts) + 1;

        splitBySize(file, sizeOfPart, destDir);
    }

    /**
     * Split by size.
     *
     * @param file
     * @param sizeOfPart
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void splitBySize(final File file, final long sizeOfPart) throws UncheckedIOException {
        splitBySize(file, sizeOfPart, file.getParentFile());
    }

    /**
     * Mostly it's designed for (zipped/unzipped/log) text files.
     *
     * @param file
     * @param sizeOfPart
     * @param destDir
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void splitBySize(final File file, final long sizeOfPart, final File destDir) throws UncheckedIOException {
        final int numOfParts = (int) ((file.length() % sizeOfPart) == 0 ? (file.length() / sizeOfPart) : (file.length() / sizeOfPart) + 1);

        final String fileName = file.getName();
        final long fileLength = file.length();
        int fileSerNum = 1;

        final byte[] buf = Objectory.createByteArrayBuffer();
        InputStream input = null;
        OutputStream output = null;

        try {
            input = IOUtil.newFileInputStream(file);

            for (int i = 0; i < numOfParts; i++) {
                final String subFileNmae = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + fileName + "_"
                        + Strings.padStart(N.stringOf(fileSerNum++), 4, '0');
                output = IOUtil.newFileOutputStream(new File(subFileNmae));
                long partLength = sizeOfPart;

                if (i == numOfParts - 1) {
                    partLength += fileLength % numOfParts;
                }

                int count = 0;

                try {
                    while (partLength > 0 && EOF != (count = read(input, buf, 0, (int) Math.min(buf.length, partLength)))) {
                        output.write(buf, 0, count);

                        partLength = partLength - count;
                    }

                    output.flush();
                } finally {
                    close(output);
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);

            closeQuietly(input);
        }
    }

    /**
     * Split by line.
     *
     * @param file
     * @param numOfParts
     * @throws UncheckedIOException the unchecked IO exception
     */
    static void splitByLine(final File file, final int numOfParts) throws UncheckedIOException {
        splitByLine(file, numOfParts, file.getParentFile());
    }

    /**
     * Mostly it's designed for (zipped/unzipped/log) text files.
     *
     * @param file
     * @param numOfParts
     * @param destDir
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("null")
    static void splitByLine(final File file, final int numOfParts, final File destDir) throws UncheckedIOException {
        N.checkArgPositive(numOfParts, cs.numOfParts);

        final int suffixLen = String.valueOf(numOfParts).length();

        final long lineNumOfPart = N.max(estimateLineCount(file, 10000) / numOfParts, 1);

        final int index = file.getName().lastIndexOf('.');
        final String prefix = file.getName().substring(0, index);
        final String postfix = (index > 0) ? file.getName().substring(index) : "";

        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        BufferedReader br = null;
        BufferedWriter bw = null;
        int fileSerNum = 1;

        try {
            is = openFile(file, outputZipFile);

            br = Objectory.createBufferedReader(is);

            String subFileNmae = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + prefix + "_" + Strings.padStart(N.stringOf(fileSerNum++), suffixLen, '0')
                    + postfix;
            bw = Objectory.createBufferedWriter(IOUtil.newFileWriter(new File(subFileNmae)));

            int lineCounter = 0;

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                bw.writeNonNull(line);
                bw.write(IOUtil.LINE_SEPARATOR);
                lineCounter++;

                if ((lineCounter % lineNumOfPart) == 0) {
                    close(bw);
                    Objectory.recycle(bw);
                    bw = null;

                    subFileNmae = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + prefix + "_" + Strings.padStart(N.stringOf(fileSerNum++), suffixLen, '0')
                            + postfix;
                    bw = Objectory.createBufferedWriter(IOUtil.newFileWriter(new File(subFileNmae)));
                }
            }

            close(bw);
            Objectory.recycle(bw);
            bw = null;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (bw != null) {
                close(bw);
                Objectory.recycle(bw);
                bw = null;
            }

            closeQuietly(is);
            close(outputZipFile.value());

            Objectory.recycle(br);
        }
    }

    /**
     * Estimate the total line count of the file by reading the specified line count ahead.
     *
     * @param file
     * @param byReadingLineNum
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    private static long estimateLineCount(final File file, final int byReadingLineNum) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;
        BufferedReader br = null;

        try {
            is = openFile(file, outputZipFile);

            br = Objectory.createBufferedReader(is);

            int cnt = 0;
            String line = null;
            long bytes = 0;
            while (cnt < byReadingLineNum && (line = br.readLine()) != null) {
                bytes += line.getBytes().length; // NOSONAR

                cnt++;
            }

            return cnt == 0 ? 0 : (file.length() / (bytes / cnt == 0 ? 1 : bytes / cnt));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeQuietly(is);
            closeQuietly(outputZipFile.value());

            Objectory.recycle(br);
        }
    }

    /**
     *
     * @param sourceFiles
     * @param destFile
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long merge(final File[] sourceFiles, final File destFile) throws UncheckedIOException {
        return merge(Array.asList(sourceFiles), destFile);
    }

    /**
     * Merge the specified source files into the destination file.
     *
     * @param sourceFiles
     * @param destFile
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long merge(final Collection<File> sourceFiles, final File destFile) throws UncheckedIOException {
        return merge(sourceFiles, N.EMPTY_BYTE_ARRAY, destFile);
    }

    /**
     * Merge the specified source files into the destination file.
     *
     * @param sourceFiles
     * @param delimiter
     * @param destFile
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long merge(final Collection<File> sourceFiles, final byte[] delimiter, final File destFile) throws UncheckedIOException {
        final byte[] buf = Objectory.createByteArrayBuffer();

        long totalCount = 0;
        OutputStream output = null;

        try {
            output = IOUtil.newFileOutputStream(destFile);

            InputStream input = null;
            int idx = 0;

            for (final File file : sourceFiles) {
                if (idx++ > 0 && N.notEmpty(delimiter)) {
                    output.write(delimiter);
                }

                try {
                    input = IOUtil.newFileInputStream(file);

                    int count = 0;
                    while (EOF != (count = read(input, buf, 0, buf.length))) {
                        output.write(buf, 0, count);

                        totalCount += count;
                    }
                } finally {
                    close(input);
                }
            }

            output.flush();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);

            close(output);
        }

        return totalCount;
    }

    /**
     * Gets the absolute path.
     *
     * @param parentDir
     * @param relativeFilePath
     * @return
     */
    private static String getAbsolutePath(final File parentDir, String relativeFilePath) {
        String newRelativePath = "";

        for (int i = 0; i < relativeFilePath.length(); i++) {
            final char c = relativeFilePath.charAt(i);

            if ((c == '\\') || (c == '/')) {
                newRelativePath += File.separator; //NOSONAR
            } else {
                newRelativePath += c; //NOSONAR
            }
        }

        relativeFilePath = newRelativePath;

        final String path = parentDir.getAbsolutePath() + File.separator + relativeFilePath;

        final File dir = new File(path.substring(0, path.lastIndexOf(File.separator)));

        if (!dir.exists()) {
            dir.mkdirs();
        }

        return path;
    }

    /**
     * Gets the relative path.
     *
     * @param parentDir
     * @param file
     * @return
     */
    private static String getRelativePath(final File parentDir, final File file) {
        if (file.equals(parentDir)) {
            return file.getName();
        } else {
            return file.getAbsolutePath().substring(parentDir.getAbsolutePath().length() + 1);
        }
    }

    /**
     *
     * @param parentPath
     * @return
     */
    public static List<String> list(final File parentPath) {
        return list(parentPath, false, false);
    }

    /**
     *
     * @param parentPath
     * @param recursively
     * @param excludeDirectory
     * @return
     */
    public static List<String> list(final File parentPath, final boolean recursively, final boolean excludeDirectory) {
        return list(parentPath, recursively, excludeDirectory ? directories_excluded_filter : all_files_filter);
    }

    /**
     * Lists the names of files in the specified parent directory.
     * If the recursively parameter is set to true, it will list files in all subdirectories as well.
     *
     * @param <E> The type of the exception that may be thrown by the filter.
     * @param parentPath The parent directory where the listing will start. It must not be {@code null}.
     * @param recursively If true, files in all subdirectories of the parent directory will be listed.
     * @param filter A BiPredicate that takes the parent directory and a file as arguments and returns a boolean. If the predicate returns true, the file is listed; if it returns false, the file is not listed.
     * @return A list of file names in the specified directory and possibly its subdirectories.
     * @throws E If the filter throws an exception.
     */
    public static <E extends Exception> List<String> list(File parentPath, final boolean recursively,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws E {
        final List<String> files = new ArrayList<>();

        if (!parentPath.exists()) {
            return files;
        }

        parentPath = new File(parentPath.getAbsolutePath().replace(".\\", "\\").replace("./", "/"));

        final File[] subFiles = parentPath.listFiles();

        if (N.isEmpty(subFiles)) {
            return files;
        }

        for (final File file : subFiles) {
            if (filter.test(parentPath, file)) {
                files.add(file.getAbsolutePath());
            }

            if (recursively && file.isDirectory()) {
                files.addAll(list(file, recursively, filter));
            }
        }

        return files;
    }

    /**
     *
     * @param parentPath
     * @return
     */
    public static List<File> listFiles(final File parentPath) {
        return listFiles(parentPath, false, false);
    }

    /**
     *
     * @param parentPath
     * @param recursively
     * @param excludeDirectory
     * @return
     */
    public static List<File> listFiles(final File parentPath, final boolean recursively, final boolean excludeDirectory) {
        return listFiles(parentPath, recursively, excludeDirectory ? directories_excluded_filter : all_files_filter);
    }

    /**
     * Lists all files in the specified parent directory.
     * If the recursively parameter is set to true, it will list files in all subdirectories as well.
     *
     * @param <E> The type of the exception that may be thrown by the filter.
     * @param parentPath The parent directory where the listing will start. It must not be {@code null}.
     * @param recursively If true, files in all subdirectories of the parent directory will be listed.
     * @param filter A BiPredicate that takes the parent directory and a file as arguments and returns a boolean. If the predicate returns true, the file is listed; if it returns false, the file is not listed.
     * @return A list of files in the specified directory and possibly its subdirectories.
     * @throws E If the filter throws an exception.
     */
    public static <E extends Exception> List<File> listFiles(final File parentPath, final boolean recursively,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws E {
        final List<File> files = new ArrayList<>();

        if (!parentPath.exists()) {
            return files;
        }

        final File[] subFiles = parentPath.listFiles();

        if (N.isEmpty(subFiles)) {
            return files;
        }

        for (final File file : subFiles) {
            if (filter.test(parentPath, file)) {
                files.add(file);
            }

            if (recursively && file.isDirectory()) {
                files.addAll(listFiles(file, recursively, filter));
            }
        }

        return files;
    }

    /**
     *
     * @param parentPath
     * @return
     */
    public static List<File> listDirectories(final File parentPath) {
        return listDirectories(parentPath, false);
    }

    /**
     *
     * @param parentPath
     * @param recursively
     * @return
     */
    public static List<File> listDirectories(final File parentPath, final boolean recursively) {
        return listFiles(parentPath, recursively, directories_only_filter);
    }

    //-----------------------------------------------------------------------
    /**
     * Convert from a <code>URL</code> to a <code>File</code>.
     * <p>
     * From version 1.1 this method will decode the URL.
     * Syntax such as <code>file:///my%20docs/file.txt</code> will be
     * correctly decoded to <code>/my docs/file.txt</code>. Starting with version
     * 1.5, this method uses UTF-8 to decode percent-encoded octets to characters.
     * Additionally, malformed percent-encoded octets are handled leniently by
     * passing them through literally.
     *
     * @param url the file URL to convert, {@code null} returns {@code null}
     * @return
     * @throws NullPointerException if the parameter is null
     */
    public static File toFile(final URL url) {
        if (!url.getProtocol().equals("file")) {
            throw new IllegalArgumentException("URL could not be converted to a File: " + url);
        }

        return new File(decodeUrl(url.getFile().replace('/', File.separatorChar)));
    }

    /**
     * Decodes the specified URL as per RFC 3986, i.e. transforms
     * percent-encoded octets to characters by decoding with the UTF-8 character
     * set. This function is primarily intended for usage with
     * {@link java.net.URL} which unfortunately does not enforce proper URLs. As
     * such, this method will leniently accept invalid characters or malformed
     * percent-encoded octets and simply pass them literally through to the
     * result string. Except for rare edge cases, this will make unencoded URLs
     * pass through unaltered.
     *
     * @param url The URL to decode, may be {@code null}.
     * @return The decoded URL or {@code null} if the input was
     *         {@code null}.
     */
    // unavoidable until Java 7
    private static String decodeUrl(final String url) {
        String decoded = url;
        if (url != null && url.indexOf('%') >= 0) {
            final int n = url.length();
            final StringBuilder buffer = new StringBuilder();
            final ByteBuffer bytes = ByteBuffer.allocate(n);
            for (int i = 0; i < n;) {
                if (url.charAt(i) == '%') {
                    try {
                        do {
                            final byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                            bytes.put(octet);
                            i += 3;
                        } while (i < n && url.charAt(i) == '%');
                    } catch (final RuntimeException e) {
                        // malformed percent-encoded octet, fall through and
                        // append characters literally
                    } finally {
                        if (bytes.position() > 0) {
                            bytes.flip();
                            buffer.append(DEFAULT_CHARSET.decode(bytes).toString());
                            bytes.clear();
                        }
                    }
                }
                buffer.append(url.charAt(i++));
            }
            decoded = buffer.toString();
        }
        return decoded;
    }

    /**
     * Converts each of an array of <code>URL</code> to a <code>File</code>.
     * <p>
     * Returns an array of the same size as the input.
     * If the input is {@code null}, an empty array is returned.
     * If the input contains {@code null}, the output array contains {@code null} at the same
     * index.
     * <p>
     * This method will decode the URL.
     * Syntax such as <code>file:///my%20docs/file.txt</code> will be
     * correctly decoded to <code>/my docs/file.txt</code>.
     *
     * @param urls the file URLs to convert, {@code null} returns empty array
     * @return a non-{@code null} array of Files matching the input, with a {@code null} item
     *  if there was a {@code null} at that index in the input array
     * @throws UncheckedIOException the unchecked IO exception
     * @throws IllegalArgumentException if any file is incorrectly encoded
     * @since 1.1
     */
    public static File[] toFiles(final URL[] urls) throws UncheckedIOException {
        if (N.isEmpty(urls)) {
            return new File[0];
        }

        final File[] files = new File[urls.length];

        for (int i = 0; i < urls.length; i++) {
            files[i] = toFile(urls[i]);
        }

        return files;
    }

    /**
     *
     * @param urls
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<File> toFiles(final Collection<URL> urls) throws UncheckedIOException {
        if (N.isEmpty(urls)) {
            return new ArrayList<>();
        }

        final List<File> files = new ArrayList<>(urls.size());

        for (final URL url : urls) {
            files.add(toFile(url));
        }

        return files;
    }

    /**
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static URL toURL(final File file) throws UncheckedIOException {
        try {
            return file.toURI().toURL();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * To UR ls.
     *
     * @param files
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static URL[] toURLs(final File[] files) throws UncheckedIOException {
        if (N.isEmpty(files)) {
            return new URL[0];
        }

        final URL[] urls = new URL[files.length];

        try {
            for (int i = 0; i < urls.length; i++) {
                urls[i] = files[i].toURI().toURL();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return urls;
    }

    /**
     * To UR ls.
     *
     * @param files
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<URL> toURLs(final Collection<File> files) throws UncheckedIOException {
        if (N.isEmpty(files)) {
            return new ArrayList<>();
        }

        final List<URL> urls = new ArrayList<>(files.size());

        try {
            for (final File file : files) {
                urls.add(file.toURI().toURL());
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return urls;
    }

    /**
     * Update the last modified time of the file to system current time if the specified file exists.
     *
     * @param source the File to touch
     * @return true if the file exists and last modified time is updated successfully.
     */
    public static boolean touch(final File source) {
        return source.exists() && source.setLastModified(System.currentTimeMillis());
    }

    /**
     * Compares the contents of two Streams to determine if they are equal or
     * not.
     * <p>
     * This method buffers the input internally using
     * {@link BufferedInputStream} if they are not already buffered.
     * </p>
     *
     * @param input1 the first stream
     * @param input2 the second stream
     * @return true if the content of the streams are equal or they both don't
     * exist, false otherwise
     * @throws IOException          if an I/O error occurs
     */
    public static boolean contentEquals(final InputStream input1, final InputStream input2) throws IOException {
        // Before making any changes, please test with
        // org.apache.commons.io.jmh.IOUtilsContentEqualsInputStreamsBenchmark
        if (input1 == input2) {
            return true;
        }

        if (input1 == null || input2 == null) {
            return false;
        }

        final byte[] buffer1 = Objectory.createByteArrayBuffer();
        final byte[] buffer2 = Objectory.createByteArrayBuffer();
        final int bufferSize = buffer1.length;

        try {
            int pos1 = 0, pos2 = 0, count1 = 0, count2 = 0;

            while (true) {
                pos1 = 0;
                pos2 = 0;

                for (int index = 0; index < bufferSize; index++) {
                    if (pos1 == index) {
                        do {
                            count1 = input1.read(buffer1, pos1, bufferSize - pos1);
                        } while (count1 == 0);

                        if (count1 == EOF) {
                            return pos2 == index && input2.read() == EOF;
                        }

                        pos1 += count1;
                    }

                    if (pos2 == index) {
                        do {
                            count2 = input2.read(buffer2, pos2, bufferSize - pos2);
                        } while (count2 == 0);

                        if (count2 == EOF) {
                            return pos1 == index && input1.read() == EOF;
                        }

                        pos2 += count2;
                    }

                    if (buffer1[index] != buffer2[index]) {
                        return false;
                    }
                }
            }
        } finally {
            Objectory.recycle(buffer1);
            Objectory.recycle(buffer2);
        }
    }

    /**
     * Compares the contents of two Readers to determine if they are equal or not.
     * <p>
     * This method buffers the input internally using {@link BufferedReader} if they are not already buffered.
     * </p>
     *
     * @param input1 the first reader
     * @param input2 the second reader
     * @return true if the content of the readers are equal or they both don't exist, false otherwise
     * @throws NullPointerException if either input is null
     * @throws IOException if an I/O error occurs
     * @since 1.1
     */
    public static boolean contentEquals(final Reader input1, final Reader input2) throws IOException {
        if (input1 == input2) {
            return true;
        }

        if (input1 == null || input2 == null) {
            return false;
        }

        final char[] buffer1 = Objectory.createCharArrayBuffer();
        final char[] buffer2 = Objectory.createCharArrayBuffer();
        final int bufferSize = buffer1.length;

        try {
            int pos1 = 0, pos2 = 0, count1 = 0, count2 = 0;

            while (true) {
                pos1 = 0;
                pos2 = 0;

                for (int index = 0; index < bufferSize; index++) {
                    if (pos1 == index) {
                        do {
                            count1 = input1.read(buffer1, pos1, bufferSize - pos1);
                        } while (count1 == 0);

                        if (count1 == EOF) {
                            return pos2 == index && input2.read() == EOF;
                        }

                        pos1 += count1;
                    }

                    if (pos2 == index) {
                        do {
                            count2 = input2.read(buffer2, pos2, bufferSize - pos2);
                        } while (count2 == 0);

                        if (count2 == EOF) {
                            return pos1 == index && input1.read() == EOF;
                        }

                        pos2 += count2;
                    }

                    if (buffer1[index] != buffer2[index]) {
                        return false;
                    }
                }
            }
        } finally {
            Objectory.recycle(buffer1);
            Objectory.recycle(buffer2);
        }
    }

    /**
     * Compares the contents of two Readers to determine if they are equal or
     * not, ignoring EOL characters.
     * <p>
     * This method buffers the input internally using
     * {@link BufferedReader} if they are not already buffered.
     * </p>
     *
     * @param input1 the first reader
     * @param input2 the second reader
     * @return true if the content of the readers are equal (ignoring EOL differences),  false otherwise
     * @throws NullPointerException if either input is null
     * @throws UncheckedIOException if an I/O error occurs
     * @since 2.2
     */
    public static boolean contentEqualsIgnoreEOL(final Reader input1, final Reader input2) throws IOException {
        if (input1 == input2) {
            return true;
        }

        if (input1 == null || input2 == null) {
            return false;
        }

        final BufferedReader br1 = input1 instanceof BufferedReader ? (BufferedReader) input1 : Objectory.createBufferedReader(input1); //NOSONAR
        final BufferedReader br2 = input2 instanceof BufferedReader ? (BufferedReader) input2 : Objectory.createBufferedReader(input2); //NOSONAR

        try {
            String line1 = br1.readLine();
            String line2 = br2.readLine();

            while (line1 != null && line2 != null && line1.equals(line2)) {
                line1 = br1.readLine();
                line2 = br2.readLine();
            }

            return line1 == null && line2 == null;
        } finally {
            if (br1 != input1) {
                Objectory.recycle(br1);
            }

            if (br2 != input2) {
                Objectory.recycle(br2);
            }
        }
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E> The type of exception that the lineParser can throw.
     * @param source The source file to be parsed.
     * @param lineParser A Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E If the lineParser throws an exception.
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final File source, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final File source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final File source, final long lineOffset, final long count, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E> The type of exception that the lineParser can throw.
     * @param <E2> The type of exception that the onComplete can throw.
     * @param source The source file/directory to be parsed.
     * @param lineOffset The line number from where to start parsing.
     * @param count The number of lines to be parsed.
     * @param processThreadNum The number of threads to be used for parsing.
     * @param queueSize The size of the queue for holding lines to be parsed.
     * @param lineParser A Consumer that takes a line of the file as a String and performs the desired operation.
     * @param onComplete A Runnable that is executed after the parsing is complete.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E If the lineParser throws an exception.
     * @throws E2 If the onComplete throws an exception.
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(source.isDirectory() ? listFiles(source, true, true) : Array.asList(source), lineOffset, count, processThreadNum, queueSize, lineParser,
                onComplete);
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E> The type of exception that the lineParser can throw.
     * @param files The collection of files to be parsed.
     * @param lineParser A Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E If the lineParser throws an exception.
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Collection<File> files, final Throwables.Consumer<? super String, E> lineParser)
            throws UncheckedIOException, E {
        parse(files, lineParser, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>
     * @param <E2>
     * @param files
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Collection<File> files, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(files, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>
     * @param files
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>
     * @param <E2>
     * @param files
     * @param lineOffset
     * @param count
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(files, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>
     * @param files
     * @param lineOffset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E> The type of exception that the lineParser can throw.
     * @param <E2> The type of exception that the onComplete can throw.
     * @param files The collection of files to be parsed.
     * @param lineOffset The line number from where to start parsing.
     * @param count The number of lines to be parsed.
     * @param lineParser A Consumer that takes a line of the file as a String and performs the desired operation.
     * @param onComplete A Runnable that is executed after the parsing is complete.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E If the lineParser throws an exception.
     * @throws E2 If the onComplete throws an exception.
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        if (N.isEmpty(files)) {
            return;
        }

        final List<Reader> readers = new ArrayList<>(files.size());

        try { //NOSONAR
            for (final File subFile : files) {
                if (subFile.isFile()) {
                    readers.add(newBufferedReader(subFile));
                } else {
                    for (final File subSubFile : listFiles(subFile, true, true)) {
                        readers.add(newBufferedReader(subSubFile));
                    }
                }
            }

            final List<Iterator<String>> iterators = new ArrayList<>(readers.size());

            for (final Reader reader : readers) {
                iterators.add(new LineIterator(reader));
            }

            Iterators.forEach(iterators, lineOffset, count, 0, processThreadNum, queueSize, lineParser, onComplete);
        } finally {
            for (final Reader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>
     * @param source
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final File source, final int readThreadNum, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final int readThreadNum, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(source, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final File source, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source parse all the sub files recursively if the element is a directory.
     * @param lineOffset
     * @param count
     * @param readThreadNum new threads started to parse/process the lines/records
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(source.isDirectory() ? listFiles(source, true, true) : Array.asList(source), lineOffset, count, readThreadNum, processThreadNum, queueSize,
                lineParser, onComplete);
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>
     * @param files
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Collection<File> files, final int readThreadNum, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>
     * @param <E2>
     * @param files
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Collection<File> files, final int readThreadNum, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(files, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>
     * @param files
     * @param lineOffset
     * @param count
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>
     * @param <E2>
     * @param files parse all the sub files recursively if the element is a directory.
     * @param lineOffset
     * @param count
     * @param readThreadNum new threads started to parse/process the lines/records
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        if (N.isEmpty(files)) {
            return;
        }

        final List<Reader> readers = new ArrayList<>(files.size());

        try { //NOSONAR
            for (final File subFile : files) {
                if (subFile.isFile()) {
                    readers.add(newBufferedReader(subFile));
                } else {
                    for (final File subSubFile : listFiles(subFile, true, true)) {
                        readers.add(newBufferedReader(subSubFile));
                    }
                }
            }

            final List<Iterator<String>> iterators = new ArrayList<>(readers.size());

            for (final Reader reader : readers) {
                iterators.add(new LineIterator(reader));
            }

            Iterators.forEach(iterators, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser, onComplete);
        } finally {
            for (final Reader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>
     * @param source
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final InputStream source, final Throwables.Consumer<? super String, E> lineParser)
            throws UncheckedIOException, E {
        parse(source, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final InputStream source, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final InputStream source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final InputStream source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final InputStream source, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineOffset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final InputStream source, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            parse(br, lineOffset, count, processThreadNum, queueSize, lineParser, onComplete);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param source
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Reader source, final Throwables.Consumer<? super String, E> lineParser)
            throws UncheckedIOException, E {
        parse(source, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Reader source, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Reader source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Reader source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @see #pase(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Reader source, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineOffset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     * @see Iterators#forEach(Iterator, long, long, int, int, com.landawn.abacus.util.Throwables.Consumer, com.landawn.abacus.util.Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Reader source, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        Iterators.forEach(new LineIterator(source), lineOffset, count, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
     *
     * @param source
     * @param outputZipFile
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("resource")
    private static InputStream openFile(final File source, final Holder<ZipFile> outputZipFile) throws IOException {
        InputStream is = null;

        if (source.getName().endsWith(GZ)) {
            is = new GZIPInputStream(IOUtil.newFileInputStream(source));
        } else if (source.getName().endsWith(ZIP)) {
            final ZipFile zf = new ZipFile(source);

            final ZipEntry ze = zf.entries().nextElement();
            is = zf.getInputStream(ze);
            outputZipFile.setValue(zf);
        } else {
            is = IOUtil.newFileInputStream(source);
        }

        return is;
    }

    /**
     *
     * @param cs
     * @param charset
     * @return
     */
    private static byte[] toByteArray(final CharSequence cs, final Charset charset) {
        return String.valueOf(cs).getBytes(charset);
    }
}
