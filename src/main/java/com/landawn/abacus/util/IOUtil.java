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
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.net.URI;
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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
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
 * @version $Revision: 0.8 $
 * @see java.nio.file.Files
 * @see com.landawn.abacus.guava.Files
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.Iterators
 */
public final class IOUtil {

    private static final Logger logger = LoggerFactory.getLogger(IOUtil.class);

    // Keep it consistent for those methods specified or not specified charset by using the default jvm charset. Should it be UTF-8?  TODO
    private static final Charset DEFAULT_CHARSET = Charsets.DEFAULT; // StandardCharsets.UTF_8;

    // ...
    private static final String JAVA_VENDOR_STR = "java.vendor";

    private static final String JAVA_VM_VENDOR_STR = "java.vm.vendor";

    private static final String ANDROID = "ANDROID";

    private static final String ZIP = ".zip";

    private static final String GZ = ".gz";

    // ...
    private static final Splitter pathSplitter = Splitter.with('/').trimResults();

    /**
     * The file copy buffer size (32 MB).
     */
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

    // public static final long ONE_ZB = 1024 * ONE_EB; // overflow

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
     *
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

    /**
     * current path retrieved by {@code new File("./").getAbsolutePath()}
     */
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
     * Retrieves the host name of the local machine.
     * If the host name cannot be determined, it returns {@code UNKNOWN_HOST_NAME}.
     *
     * @return The host name of the local machine. If the host name cannot be determined, it returns {@code UNKNOWN_HOST_NAME}.
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
                    //noinspection resource
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
     * @throws UncheckedIOException  if an error occurs when finding the free space
     * @throws IllegalStateException if an error occurred in initialization
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
     * @param timeout The timeout amount in milliseconds or no timeout if the value is zero or less
     * @return
     * @throws UncheckedIOException  if an error occurs when finding the free space
     * @throws IllegalStateException if an error occurred in initialization
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
     * In order to work, you must be running Windows, or have an implementation of
     * Unix df that supports GNU format when passed -k (or -kP). If you are going
     * to rely on this code, please check that it works on your OS by running
     * some simple tests to compare the command line with the output from this class.
     * If your operating system isn't supported, please raise a JIRA call detailing
     * the exact result from df -k and as much other detail as possible, thanks.
     *
     * @param path the path to get free space for, not {@code null}, not empty on Unix
     * @return
     * @throws UncheckedIOException     if an error occurs when finding the free space
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException    if an error occurred in initialization
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
     * In order to work, you must be running Windows, or have an implementation of
     * Unix df that supports GNU format when passed -k (or -kP). If you are going
     * to rely on this code, please check that it works on your OS by running
     * some simple tests to compare the command line with the output from this class.
     * If your operating system isn't supported, please raise a JIRA call detailing
     * the exact result from df -k and as much other detail as possible, thanks.
     *
     * @param path    the path to get free space for, not {@code null}, not empty on Unix
     * @param timeout The timeout amount in milliseconds or no timeout if the value
     *                is zero or less
     * @return
     * @throws UncheckedIOException     if an error occurs when finding the free space
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException    if an error occurred in initialisation
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
     * Converts an array of characters into an array of bytes using the default charset.
     *
     * @param chars The array of characters to be converted, not {@code null}.
     * @return The resulting byte array.
     */
    public static byte[] chars2Bytes(final char[] chars) {
        return chars2Bytes(chars, DEFAULT_CHARSET);
    }

    /**
     * Converts an array of characters into an array of bytes using the provided charset.
     *
     * @param chars   The array of characters to be converted, not {@code null}.
     * @param charset The charset to be used to open the specified source for reading.
     * @return The resulting byte array.
     */
    public static byte[] chars2Bytes(final char[] chars, final Charset charset) {
        if (N.isEmpty(chars)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return chars2Bytes(chars, 0, chars.length, charset);
    }

    /**
     * Converts an array of characters into an array of bytes using the provided charset.
     *
     * @param chars     The array of characters to be converted, not {@code null}.
     * @param offset    The index of the first character in the array to convert.
     * @param charCount The number of characters to convert.
     * @param charset   The charset to be used to open the specified source for reading.
     * @return The resulting byte array.
     */
    public static byte[] chars2Bytes(final char[] chars, final int offset, final int charCount, Charset charset) {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(charCount, cs.count);

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
     * Converts an array of bytes into an array of characters using the default charset.
     *
     * @param bytes The array of bytes to be converted, not {@code null}.
     * @return The resulting character array.
     */
    public static char[] bytes2Chars(final byte[] bytes) {
        return bytes2Chars(bytes, DEFAULT_CHARSET);
    }

    /**
     * Converts an array of bytes into an array of characters using the provided charset.
     *
     * @param bytes   The array of bytes to be converted, not {@code null}.
     * @param charset The charset to be used to open the specified source for reading.
     * @return The resulting character array.
     */
    public static char[] bytes2Chars(final byte[] bytes, final Charset charset) {
        if (N.isEmpty(bytes)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return bytes2Chars(bytes, 0, bytes.length, charset);
    }

    /**
     * Converts an array of bytes into an array of characters using the provided charset.
     *
     * @param bytes     The array of bytes to be converted, not {@code null}.
     * @param offset    The index of the first byte in the array to convert.
     * @param byteCount The number of bytes to convert.
     * @param charset   The charset to be used to open the specified source for reading.
     * @return The resulting character array.
     */
    public static char[] bytes2Chars(final byte[] bytes, final int offset, final int byteCount, Charset charset) {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(byteCount, cs.count);

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
     * Converts a string into an InputStream using the default charset.
     *
     * @param str The string to be converted, not {@code null}.
     * @return The resulting InputStream.
     * @see #string2InputStream(String, Charset)
     * @see String#getBytes()
     */
    public static InputStream string2InputStream(final String str) {
        return string2InputStream(str, DEFAULT_CHARSET);
    }

    /**
     * Converts a string into an InputStream using the provided charset.
     *
     * @param str     The string to be converted, not {@code null}.
     * @param charset The charset to be used to open the specified source for reading.
     * @return The resulting InputStream.
     * @see String#getBytes(Charset)
     */
    public static InputStream string2InputStream(final String str, Charset charset) {
        if (str == null) {
            throw new IllegalArgumentException("The input String can't be null.");
        }

        charset = checkCharset(charset);

        return new ByteArrayInputStream(str.getBytes(charset));
    }

    /**
     * Converts a string into a Reader.
     *
     * @param str The string to be converted.
     * @return The resulting Reader.
     * @see StringReader
     */
    public static Reader string2Reader(final String str) {
        return new StringReader(Strings.nullToEmpty(str));
    }

    /**
     * Converts a StringBuilder into a Writer.
     *
     * @param sb The StringBuilder to be converted, not {@code null}.
     * @return The resulting Writer.
     * @throws IllegalArgumentException if the input StringBuilder is {@code null}.
     */
    public static Writer stringBuilder2Writer(final StringBuilder sb) throws IllegalArgumentException {
        if (sb == null) {
            throw new IllegalArgumentException("The input StringBuilder can't be null.");
        }

        return new StringWriter(sb);
    }

    /**
     * Reads all bytes from a file into a byte array.
     * <br />
     * Comparing with {@code readbytes(File)}, this method throws {@code UncheckedIOException} instead of {@code IOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream} with size closed to {@code Integer.MAX_VALUE}
     *
     * @param source The file to read bytes from, not {@code null}.
     * @return A byte array containing all bytes from the file.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
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
     * Reads all bytes from an InputStream into a byte array.
     * <br />
     * Comparing with {@code readbytes(InputStream)}, this method throws {@code UncheckedIOException} instead of {@code IOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return A byte array containing all bytes from the file.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static byte[] readAllBytes(final InputStream source) throws UncheckedIOException {
        try {
            return readBytes(source, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads all bytes from a file into a byte array.
     * <br />
     * Comparing with {@code readAllBytes(File)}, this method throws {@code IOException} instead of {@code UncheckedIOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The file to read bytes from, not {@code null}.
     * @return A byte array containing all bytes from the file.
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException      if an I/O error occurs. the unchecked IO exception
     * @see #readAllBytes(File)
     */
    @Beta
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
     * Reads a specified number of bytes from a file into a byte array.
     * The reading starts from the specified offset in bytes in the file.
     *
     * @param source The file to read bytes from, not {@code null}.
     * @param offset The starting position in bytes in the file, from where to start reading.
     * @param maxLen The maximum number of bytes to read.
     * @return A byte array containing the read bytes from the file.
     * @throws IOException if an I/O error occurs.
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
     * Reads all bytes from an InputStream into a byte array.
     * <br />
     * Comparing with {@code readAllBytes(InputStream)}, this method throws {@code IOException} instead of {@code UncheckedIOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return A byte array containing all bytes from the file.
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException      if an I/O error occurs.
     * @see #readAllBytes(InputStream)
     */
    @Beta
    public static byte[] readBytes(final InputStream source) throws IOException {
        return readBytes(source, 0, Long.MAX_VALUE);
    }

    /**
     * Reads a specified number of bytes from an InputStream into a byte array.
     * The reading starts from the specified offset in bytes in the InputStream.
     *
     * @param source The InputStream to read bytes from, not {@code null}.
     * @param offset The starting position in bytes in the InputStream, from where to start reading.
     * @param maxLen The maximum number of bytes to read.
     * @return A byte array containing the read bytes from the InputStream.
     * @throws IOException if an I/O error occurs.
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
     * Reads all characters from a file into a character array using the default charset.
     * <br />
     * Comparing with {@code readChars(File)}, this method throws {@code UncheckedIOException} instead of {@code IOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The file to read characters from, not {@code null}.
     * @return A character array containing all characters from the file.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static char[] readAllChars(final File source) throws UncheckedIOException {
        return readAllChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all characters from a file into a character array using the provided charset.
     * <br />
     * Comparing with {@code readChars(File, Charset)}, this method throws {@code UncheckedIOException} instead of {@code IOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source   The file to read characters from, not {@code null}.
     * @param encoding The charset to be used for conversion, not {@code null}.
     * @return A character array containing all characters from the file.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
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
     * Reads all characters from an InputStream into a character array using the default charset.
     * <br />
     * Comparing with {@code readChars(InputStream)}, this method throws {@code UncheckedIOException} instead of {@code IOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The InputStream to read characters from, not {@code null}.
     * @return A character array containing all characters from the InputStream.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static char[] readAllChars(final InputStream source) throws UncheckedIOException {
        return readAllChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all characters from an InputStream into a character array using the provided charset.
     * <br />
     * Comparing with {@code readChars(InputStream, Charset)}, this method throws {@code UncheckedIOException} instead of {@code IOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source   The InputStream to read characters from, not {@code null}.
     * @param encoding The charset to be used for conversion, not {@code null}.
     * @return A character array containing all characters from the InputStream.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
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
     * Reads all characters from a Reader into a character array.
     * <br />
     * Comparing with {@code readChars(Reader)}, this method throws {@code UncheckedIOException} instead of {@code IOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The Reader to read characters from, not {@code null}.
     * @return A character array containing all characters from the Reader.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static char[] readAllChars(final Reader source) throws UncheckedIOException {
        try {
            return readChars(source, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads all characters from a file into a character array using the default charset.
     * <br />
     * Comparing with {@code readAllChars(File)}, this method throws {@code IOException} instead of {@code UncheckedIOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The file to read characters from, not {@code null}.
     * @return A character array containing all characters from the file.
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException      if an I/O error occurs.
     * @see #readAllChars(File)
     */
    @Beta
    public static char[] readChars(final File source) throws IOException {
        return readChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all characters from a file into a character array using the provided charset.
     * <br />
     * Comparing with {@code readAllChars(File, Charset)}, this method throws {@code IOException} instead of {@code UncheckedIOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source   The file to read characters from, not {@code null}.
     * @param encoding The charset to be used for conversion, not {@code null}.
     * @return A character array containing all characters from the file.
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException      if an I/O error occurs.
     * @see #readAllChars(File, Charset)
     */
    @Beta
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
     * Reads a specified number of characters from a file into a character array using the default charset.
     * The reading starts from the specified offset in characters in the file.
     *
     * @param source The file to read characters from, not {@code null}.
     * @param offset The starting position in characters in the file, from where to start reading.
     * @param maxLen The maximum number of characters to read.
     * @return A character array containing the read characters from the file.
     * @throws IOException if an I/O error occurs.
     */
    public static char[] readChars(final File source, final long offset, final int maxLen) throws IOException {
        return readChars(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads a specified number of characters from a file into a character array using the provided charset.
     * The reading starts from the specified offset in characters in the file.
     *
     * @param source   The file to read characters from, not {@code null}.
     * @param encoding The charset to be used for conversion, not {@code null}.
     * @param offset   The starting position in characters in the file, from where to start reading.
     * @param maxLen   The maximum number of characters to read.
     * @return A character array containing the read characters from the file.
     * @throws IOException if an I/O error occurs.
     */
    public static char[] readChars(final File source, final Charset encoding, final long offset, final int maxLen) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

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
     * Reads all characters from an InputStream into a character array using the default charset.
     * <br />
     * Comparing with {@code readAllChars(InputStream)}, this method throws {@code IOException} instead of {@code UncheckedIOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The InputStream to read characters from, not {@code null}.
     * @return A character array containing all characters from the InputStream.
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException      if an I/O error occurs.
     * @see #readAllChars(InputStream)
     */
    @Beta
    public static char[] readChars(final InputStream source) throws IOException {
        return readChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all characters from an InputStream into a character array using the provided charset.
     * <br />
     * Comparing with {@code readAllChars(InputStream, Charset)}, this method throws {@code IOException} instead of {@code UncheckedIOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source   The InputStream to read characters from, not {@code null}.
     * @param encoding The charset to be used for conversion, not {@code null}.
     * @return A character array containing all characters from the InputStream.
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException      if an I/O error occurs.
     * @see #readAllChars(InputStream, Charset)
     */
    @Beta
    public static char[] readChars(final InputStream source, final Charset encoding) throws IOException {
        final Reader reader = createReader(source, encoding);

        //    try {
        return readChars(reader, 0, Long.MAX_VALUE);
        //    } finally {
        //        close(reader);
        //    }
    }

    /**
     * Reads a specified number of characters from an InputStream into a character array using the default charset.
     * The reading starts from the specified offset in characters in the InputStream.
     *
     * @param source The InputStream to read characters from, not {@code null}.
     * @param offset The starting position in characters in the InputStream, from where to start reading.
     * @param maxLen The maximum number of characters to read.
     * @return A character array containing the read characters from the InputStream.
     * @throws IOException if an I/O error occurs.
     */
    public static char[] readChars(final InputStream source, final long offset, final int maxLen) throws IOException {
        return readChars(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads a specified number of characters from an InputStream into a character array using the provided charset.
     * The reading starts from the specified offset in characters in the InputStream.
     *
     * @param source   The InputStream to read characters from, not {@code null}.
     * @param encoding The charset to be used for conversion, not {@code null}.
     * @param offset   The starting position in characters in the InputStream, from where to start reading.
     * @param maxLen   The maximum number of characters to read.
     * @return A character array containing the read characters from the InputStream.
     * @throws IOException if an I/O error occurs.
     */
    public static char[] readChars(final InputStream source, Charset encoding, final long offset, final int maxLen) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        encoding = checkCharset(encoding);

        // try {
        final Reader reader = createReader(source, encoding);
        // } finally {
        // // close(reader);
        // }

        return readChars(reader, offset, maxLen);
    }

    /**
     * Reads all characters from a Reader into a character array using the default charset.
     * <br />
     * Comparing with {@code readAllChars(Reader)}, this method throws {@code IOException} instead of {@code UncheckedIOException}.
     * <p>
     * <br />
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The Reader to read characters from, not {@code null}.
     * @return A character array containing all characters from the Reader.
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     * @throws IOException      if an I/O error occurs.
     * @see #readAllChars(Reader)
     */
    @Beta
    public static char[] readChars(final Reader source) throws IOException {
        return readChars(source, 0, Long.MAX_VALUE);
    }

    /**
     * Reads a specified number of characters from a Reader into a character array.
     * The reading starts from the specified offset in characters in the Reader.
     *
     * @param source The Reader to read characters from, not {@code null}.
     * @param offset The starting position in characters in the Reader, from where to start reading.
     * @param maxLen The maximum number of characters to read.
     * @return A character array containing the read characters from the Reader.
     * @throws IOException if an I/O error occurs.
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
     * Reads all content from a file into a String using the default charset.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The file to read content from, not {@code null}.
     * @return A String containing all content from the file.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static String readAllToString(final File source) throws UncheckedIOException {
        return readAllToString(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all content from a file into a String using the provided charset.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source   The file to read content from, not {@code null}.
     * @param encoding The charset to be used to open the specified file for reading.
     * @return A String containing all content from the file.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static String readAllToString(final File source, final Charset encoding) throws UncheckedIOException {
        final byte[] bytes = readAllBytes(source);

        return new String(bytes, checkCharset(encoding));
    }

    /**
     * Reads all content from an InputStream into a String using the default charset.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The InputStream to read content from, not {@code null}.
     * @return A String containing all content from the InputStream.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static String readAllToString(final InputStream source) throws UncheckedIOException {
        return readAllToString(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all content from an InputStream into a String using the provided charset.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source   The InputStream to read content from, not {@code null}.
     * @param encoding The charset to be used to open the specified InputStream for reading.
     * @return A String containing all content from the InputStream.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static String readAllToString(final InputStream source, final Charset encoding) throws UncheckedIOException {
        final byte[] bytes = readAllBytes(source);

        return new String(bytes, checkCharset(encoding));
    }

    /**
     * Reads all content from a Reader into a String.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The Reader to read content from, not {@code null}.
     * @return A String containing all content from the Reader.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static String readAllToString(final Reader source) throws UncheckedIOException {
        final char[] chars = readAllChars(source);

        return String.valueOf(chars);
    }

    /**
     * Reads a specified number of characters from a file into a String using the default charset.
     * The reading starts from the specified offset in characters in the file.
     *
     * @param source The file to read characters from, not {@code null}.
     * @param offset The starting position in characters in the file, from where to start reading.
     * @param maxLen The maximum number of characters to read.
     * @return A String containing the read characters from the file.
     * @throws IOException if an I/O error occurs.
     */
    public static String readToString(final File source, final long offset, final int maxLen) throws IOException {
        return readToString(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads a specified number of characters from a file into a String using the provided charset.
     * The reading starts from the specified offset in characters in the file.
     *
     * @param source   The file to read characters from, not {@code null}.
     * @param encoding The charset to be used to open the specified file for reading.
     * @param offset   The starting position in characters in the file, from where to start reading.
     * @param maxLen   The maximum number of characters to read.
     * @return A String containing the read characters from the file.
     * @throws IOException if an I/O error occurs.
     */
    public static String readToString(final File source, final Charset encoding, final long offset, final int maxLen) throws IOException {
        final byte[] bytes = readBytes(source, offset, maxLen);

        return new String(bytes, checkCharset(encoding));
    }

    /**
     * Reads a specified number of characters from an InputStream into a String using the default charset.
     * The reading starts from the specified offset in characters in the InputStream.
     *
     * @param source The InputStream to read characters from, not {@code null}.
     * @param offset The starting position in characters in the InputStream, from where to start reading.
     * @param maxLen The maximum number of characters to read.
     * @return A String containing the read characters from the InputStream.
     * @throws IOException if an I/O error occurs.
     */
    public static String readToString(final InputStream source, final long offset, final int maxLen) throws IOException {
        return readToString(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads a specified number of characters from an InputStream, starting from a specified offset in characters, and converts them into a String.
     * The characters are decoded using the provided Charset.
     *
     * @param source   The InputStream to read from.
     * @param encoding The charset to be used to open the specified InputStream for reading.
     * @param offset   The position in characters in the InputStream to start reading from.
     * @param maxLen   The maximum number of characters to read from the InputStream.
     * @return A String containing the characters read from the InputStream.
     * @throws IOException if an I/O error occurs.
     */
    public static String readToString(final InputStream source, final Charset encoding, final long offset, final int maxLen) throws IOException {
        final byte[] bytes = readBytes(source, offset, maxLen);

        return new String(bytes, checkCharset(encoding));
    }

    /**
     * Reads a specified number of characters from a Reader into a String.
     * The reading starts from the specified offset in characters in the Reader.
     *
     * @param source The Reader to read characters from, not {@code null}.
     * @param offset The starting position in characters in the Reader, from where to start reading.
     * @param maxLen The maximum number of characters to read.
     * @return A String containing the read characters from the Reader.
     * @throws IOException if an I/O error occurs.
     */
    public static String readToString(final Reader source, final long offset, final int maxLen) throws IOException {
        final char[] chs = readChars(source, offset, maxLen);

        return String.valueOf(chs);
    }

    /**
     * Reads all lines from a file and returns them as a List of Strings using the default charset.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with line size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source The InputStream to read from.
     * @return A List of Strings, each string being a line from the InputStream.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public static List<String> readAllLines(final File source) throws UncheckedIOException {
        return readAllLines(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all lines from a file and returns them as a List of Strings using the provided charset.
     * <br />
     * Note: This method should not be used to read a {@code File} with line size close to {@code Integer.MAX_VALUE}.
     *
     * @param source   The file to read lines from, not {@code null}.
     * @param encoding The charset to be used for conversion, not {@code null}.
     * @return A List of Strings, each string being a line from the file.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException If an I/O error occurs.
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
     * Reads all lines from an InputStream and returns them as a List of Strings using the default charset.
     * <br />
     * Note: This method should not be used to read an {@code InputStream} with line size close to {@code Integer.MAX_VALUE}.
     *
     * @param source The InputStream to read lines from, not {@code null}.
     * @return A List of Strings, each string being a line from the InputStream.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public static List<String> readAllLines(final InputStream source) throws UncheckedIOException {
        return readAllLines(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all lines from an InputStream and returns them as a List of Strings using the provided charset.
     * <br />
     * Note: This method should not be used to read an {@code InputStream} with line size close to {@code Integer.MAX_VALUE}.
     *
     * @param source   The InputStream to read lines from, not {@code null}.
     * @param encoding The charset to be used for conversion, not {@code null}.
     * @return A List of Strings, each string being a line from the InputStream.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException If an I/O error occurs.
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
     * Reads all lines from a Reader and returns them as a List of Strings.
     * <br />
     * Note: This method should not be used to read a {@code Reader} with line size close to {@code Integer.MAX_VALUE}.
     *
     * @param source The Reader to read lines from, not {@code null}.
     * @return A List of Strings, each string being a line from the Reader.
     * @throws OutOfMemoryError     if an array of the required size cannot be allocated
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public static List<String> readAllLines(final Reader source) throws UncheckedIOException {
        final List<String> res = new ArrayList<>();
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR

        try {
            String line = null;

            while ((line = br.readLine()) != null) { //NOSONAR
                res.add(line);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }

        return res;
    }

    /**
     * Reads the first line from a file using the default charset and returns it as a String.
     *
     * @param source The file to read the first line from, not {@code null}.
     * @return A String containing the first line from the file, or {@code null} if the file is empty.
     * @throws IOException if an I/O error occurs.
     */
    public static String readFirstLine(final File source) throws IOException {
        return readFirstLine(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the first line from a file using the provided charset and returns it as a String.
     *
     * @param source   The file to read the first line from, not {@code null}.
     * @param encoding The charset to be used the open the specified file for reading.
     * @return A String containing the first line from the file, or {@code null} if the file is empty.
     * @throws IOException if an I/O error occurs.
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
     * Reads the first line from an InputStream using the default charset and returns it as a String.
     *
     * @param source The InputStream to read the first line from, not {@code null}.
     * @return A String containing the first line from the InputStream, or {@code null} if the InputStream is empty.
     * @throws IOException if an I/O error occurs.
     */
    public static String readFirstLine(final InputStream source) throws IOException {
        return readFirstLine(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the first line from an InputStream using the provided charset and returns it as a String.
     *
     * @param source   The InputStream to read the first line from, not {@code null}.
     * @param encoding The charset to be used the open the specified InputStream for reading.
     * @return A String containing the first line from the InputStream, or {@code null} if the InputStream is empty.
     * @throws IOException if an I/O error occurs.
     */
    public static String readFirstLine(final InputStream source, final Charset encoding) throws IOException {
        return readFirstLine(createReader(source, encoding));
    }

    /**
     * Reads the first line from a Reader and returns it as a String.
     *
     * @param source The Reader to read the first line from, not {@code null}.
     * @return A String containing the first line from the Reader, or {@code null} if the Reader is empty.
     * @throws IOException if an I/O error occurs.
     */
    public static String readFirstLine(final Reader source) throws IOException {
        return readLine(source, 0);
    }

    /**
     * Reads the last line from a file using the default charset and returns it as a String.
     *
     * @param source The file to read the last line from, not {@code null}.
     * @return A String containing the last line from the file, or {@code null} if the file is empty.
     * @throws IOException if an I/O error occurs.
     */
    public static String readLastLine(final File source) throws IOException {
        return readLastLine(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the last line from a file using the provided charset and returns it as a String.
     *
     * @param source   The file to read the last line from, not {@code null}.
     * @param encoding The charset to be used the open the specified file for reading.
     * @return A String containing the last line from the file, or {@code null} if the file is empty.
     * @throws IOException if an I/O error occurs.
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
     * Reads the last line from an InputStream using the default charset and returns it as a String.
     *
     * @param source The InputStream to read the last line from, not {@code null}.
     * @return A String containing the last line from the InputStream, or {@code null} if the InputStream is empty.
     * @throws IOException if an I/O error occurs.
     */
    public static String readLastLine(final InputStream source) throws IOException {
        return readLastLine(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the last line from an InputStream using the provided charset and returns it as a String.
     *
     * @param source   The InputStream to read the last line from, not {@code null}.
     * @param encoding The charset to be used the open the specified InputStream for reading.
     * @return A String containing the last line from the InputStream, or {@code null} if the InputStream is empty.
     * @throws IOException if an I/O error occurs.
     */
    public static String readLastLine(final InputStream source, final Charset encoding) throws IOException {
        return readLastLine(createReader(source, encoding));
    }

    /**
     * Reads the last line from a Reader and returns it as a String.
     *
     * @param source The Reader to read the last line from, not {@code null}.
     * @return A String containing the last line from the Reader, or {@code null} if the Reader is empty.
     * @throws IOException if an I/O error occurs.
     */
    public static String readLastLine(final Reader source) throws IOException {
        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR

        try {
            String ret = null;
            String line = null;

            while ((line = br.readLine()) != null) { //NOSONAR
                ret = line;
            }

            return ret;
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Reads a specific line from a file using the default charset and returns it as a String.
     * The line to read is determined by the provided line index (0-based).
     *
     * @param source    The file to read the line from, not {@code null}.
     * @param lineIndex The index of the line to read, starting from 0 for the first line.
     * @return A String containing the specified line from the file, or {@code null} if the file has fewer lines.
     * @throws IllegalArgumentException if the line index is negative.
     * @throws IOException              if an I/O error occurs.
     */
    public static String readLine(final File source, final int lineIndex) throws IllegalArgumentException, IOException {
        return readLine(source, DEFAULT_CHARSET, lineIndex);
    }

    /**
     * Reads a specific line from a file using the provided charset and returns it as a String.
     * The line to read is determined by the provided line index (0-based).
     *
     * @param source    The file to read the line from, not {@code null}.
     * @param encoding  The charset to be used the open the specified file for reading.
     * @param lineIndex The index of the line to read, starting from 0 for the first line.
     * @return A String containing the specified line from the file, or {@code null} if the file has fewer lines.
     * @throws IllegalArgumentException if the line index is negative.
     * @throws IOException              if an I/O error occurs.
     */
    public static String readLine(final File source, final Charset encoding, final int lineIndex) throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(lineIndex, cs.lineIndex);

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
     * Reads a specific line from an InputStream using the default charset and returns it as a String.
     * The line to read is determined by the provided line index (0-based).
     *
     * @param source    The InputStream to read the line from, not {@code null}.
     * @param lineIndex The index of the line to read, starting from 0 for the first line.
     * @return A String containing the specified line from the InputStream, or {@code null} if the InputStream has fewer lines.
     * @throws IllegalArgumentException if the line index is negative.
     * @throws IOException              if an I/O error occurs.
     */
    public static String readLine(final InputStream source, final int lineIndex) throws IllegalArgumentException, IOException {
        return readLine(source, DEFAULT_CHARSET, lineIndex);
    }

    /**
     * Reads a specific line from an InputStream using the provided charset and returns it as a String.
     * The line to read is determined by the provided line index (0-based).
     *
     * @param source    The InputStream to read the line from, not {@code null}.
     * @param encoding  The charset to be used the open the specified InputStream for reading.
     * @param lineIndex The index of the line to read, starting from 0 for the first line.
     * @return A String containing the specified line from the InputStream, or {@code null} if the InputStream has fewer lines.
     * @throws IllegalArgumentException if the line index is negative.
     * @throws IOException              if an I/O error occurs.
     */
    public static String readLine(final InputStream source, final Charset encoding, final int lineIndex) throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(lineIndex, cs.lineIndex);

        return readLine(createReader(source, encoding), lineIndex);
    }

    /**
     * Reads a specific line from a Reader and returns it as a String.
     * The line to read is determined by the provided line index (0-based).
     *
     * @param source    The Reader to read the line from, not {@code null}.
     * @param lineIndex The index of the line to read, starting from 0 for the first line.
     * @return A String containing the specified line from the Reader, or {@code null} if the Reader has fewer lines.
     * @throws IllegalArgumentException if the line index is negative.
     * @throws IOException              if an I/O error occurs.
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static String readLine(final Reader source, int lineIndex) throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(lineIndex, cs.lineIndex);

        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR

        try {
            if ((lineIndex > 0)) {
                while (lineIndex-- > 0 && br.readLine() != null) { // NOSONAR
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
            if (!isBufferedReader) {
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
    //     * @throws UncheckedIOException if an I/O error occurs.
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
    //     * @throws UncheckedIOException if an I/O error occurs.
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
    //     * @throws UncheckedIOException if an I/O error occurs.
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
    //     * @throws UncheckedIOException if an I/O error occurs.
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
    //     * @throws UncheckedIOException if an I/O error occurs.
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
     * Reads a specified number of lines from a file using the default charset and returns them as a List of Strings.
     * The lines to read are determined by the provided offset (0-based) and count.
     *
     * @param source The file to read lines from, not {@code null}.
     * @param offset The starting position in lines in the file, from where to start reading.
     * @param count  The number of lines to read from the file.
     * @return A List of Strings, each string being a line from the file.
     * @throws IOException if an I/O error occurs.
     */
    public static List<String> readLines(final File source, final int offset, final int count) throws IOException {
        return readLines(source, DEFAULT_CHARSET, offset, count);
    }

    /**
     * Reads a specified number of lines from a file using the provided charset and returns them as a List of Strings.
     * The lines to read are determined by the provided offset (0-based) and count.
     *
     * @param source   The file to read lines from, not {@code null}.
     * @param encoding The charset to be used to open the specified file for reading.
     * @param offset   The starting position in lines in the file, from where to start reading.
     * @param count    The number of lines to read from the file.
     * @return A List of Strings, each string being a line from the file.
     * @throws IOException if an I/O error occurs.
     */
    public static List<String> readLines(final File source, final Charset encoding, final int offset, final int count) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

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
     * Reads a specified number of lines from an InputStream using the default charset  and returns them as a List of Strings.
     * The lines to read are determined by the provided offset (0-based) and count.
     *
     * @param source The InputStream to read lines from, not {@code null}.
     * @param offset The starting position in lines in the InputStream, from where to start reading.
     * @param count  The number of lines to read from the InputStream.
     * @return A List of Strings, each string being a line from the InputStream.
     * @throws IOException if an I/O error occurs.
     */
    public static List<String> readLines(final InputStream source, final int offset, final int count) throws IOException {
        return readLines(source, DEFAULT_CHARSET, offset, count);
    }

    /**
     * Reads a specified number of lines from an InputStream using the provided charset  and returns them as a List of Strings.
     * The lines to read are determined by the provided offset (0-based) and count.
     *
     * @param source   The InputStream to read lines from, not {@code null}.
     * @param encoding The charset to be used to open the specified InputStream for reading
     * @param offset   The starting position in lines in the InputStream, from where to start reading.
     * @param count    The number of lines to read from the InputStream.
     * @return A List of Strings, each string being a line from the InputStream.
     * @throws IOException if an I/O error occurs.
     */
    public static List<String> readLines(final InputStream source, final Charset encoding, final int offset, final int count) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        return readLines(createReader(source, encoding), offset, count);
    }

    /**
     * Reads a specified number of lines from a Reader and returns them as a List of Strings.
     * The lines to read are determined by the provided offset (0-based) and count.
     *
     * @param source The Reader to read lines from, not {@code null}.
     * @param offset The starting position in lines in the Reader, from where to start reading.
     * @param count  The number of lines to read from the Reader.
     * @return A List of Strings, each string being a line from the Reader.
     * @throws IOException if an I/O error occurs.
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static List<String> readLines(final Reader source, int offset, int count) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR
        final List<String> res = new ArrayList<>();

        try {
            while (offset-- > 0 && br.readLine() != null) { //NOSONAR
                // continue
            }

            String line = null;

            while (count-- > 0 && (line = br.readLine()) != null) { //NOSONAR
                res.add(line);
            }

        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }

        return res;
    }

    /**
     * Reads data from a file into a byte array buffer.
     *
     * @param source The file to read from.
     * @param buf    The byte array buffer where the data is to be stored.
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final byte[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a byte array buffer.
     *
     * @param source The file to read data from, not {@code null}.
     * @param buf    The byte array buffer where the data is to be stored, not {@code null}.
     * @param off    The start offset in the array at which the data is written.
     * @param len    The maximum number of bytes to read.
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
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
     * Reads data from an InputStream into a byte array buffer.
     *
     * @param source The InputStream to read data from, not {@code null}.
     * @param buf    The byte array buffer where the data is to be stored, not {@code null}.
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final InputStream source, final byte[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from an InputStream into a byte array buffer.
     *
     * @param source The InputStream to read data from, not {@code null}.
     * @param buf    The byte array buffer where the data is to be stored, not {@code null}.
     * @param off    The start offset in the array at which the data is written.
     * @param len    The maximum number of bytes to read.
     * @return The total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
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
     * Reads data from a file into a char array buffer using the default charset.
     *
     * @param source The file to read data from, not {@code null}.
     * @param buf    The char array buffer where the data is to be stored, not {@code null}.
     * @return The total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final char[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a char array buffer using the provided charset.
     *
     * @param source  The file to read data from, not {@code null}.
     * @param charset The charset to be used to open the specified file for reading.
     * @param buf     The char array buffer where the data is to be stored, not {@code null}.
     * @return The total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final Charset charset, final char[] buf) throws IOException {
        return read(source, charset, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a char array buffer using the default charset.
     *
     * @param source The file to read data from, not {@code null}.
     * @param buf    The char array buffer where the data is to be stored, not {@code null}.
     * @param off    The start offset in the array at which the data is written.
     * @param len    The maximum number of chars to read.
     * @return The total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final char[] buf, final int off, final int len) throws IOException {
        return read(source, DEFAULT_CHARSET, buf, off, len);
    }

    /**
     * Reads data from a file into a char array buffer using the provided charset.
     *
     * @param source  The file to read data from, not {@code null}.
     * @param charset The charset to be used to open the specified file for reading.
     * @param buf     The char array buffer where the data is to be stored, not {@code null}.
     * @param off     The start offset in the array at which the data is written.
     * @param len     The maximum number of chars to read.
     * @return The total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final Charset charset, final char[] buf, final int off, final int len) throws IOException {
        Reader reader = null;

        try { //NOSONAR
            reader = IOUtil.newFileReader(source, checkCharset(charset));

            return read(reader, buf, off, len);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     * Reads data from a Reader into a char array buffer.
     *
     * @param source The Reader to read data from, not {@code null}.
     * @param buf    The char array buffer where the data is to be stored, not {@code null}.
     * @return The total number of chars read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final Reader source, final char[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from a Reader into a char array buffer.
     *
     * @param source The Reader to read data from, not {@code null}.
     * @param buf    The char array buffer where the data is to be stored, not {@code null}.
     * @param off    The start offset in the array at which the data is written.
     * @param len    The maximum number of chars to read.
     * @return The total number of chars read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
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
     * Writes the string representation of an Object to a file as a single line using the default charset.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     *
     * @param obj    The Object to be written.
     * @param output The file where the object's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
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
     * Writes the string representation of an Object to an OutputStream as a single line using the default charset.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     *
     * @param obj    The Object to be written.
     * @param output The OutputStream where the object's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLine(final Object obj, final OutputStream output) throws IOException {
        writeLine(obj, output, false);
    }

    /**
     * Writes the string representation of an Object to an OutputStream as a single line using the default charset.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     *
     * @param obj    The Object to be written.
     * @param output The OutputStream where the object's string representation is to be written, not {@code null}.
     * @param flush  If {@code true}, the stream will be flushed after writing the line.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLine(final Object obj, final OutputStream output, final boolean flush) throws IOException {
        writeLine(obj, IOUtil.newOutputStreamWriter(output), flush); // NOSONAR
    }

    /**
     * Writes the string representation of an Object to a Writer as a single line using the default charset.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     *
     * @param obj    The Object to be written.
     * @param output The Writer where the object's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLine(final Object obj, final Writer output) throws IOException {
        writeLine(obj, output, false);
    }

    /**
     * Writes the string representation of an Object to a Writer as a single line.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     *
     * @param obj    The Object to be written.
     * @param output The Writer where the object's string representation is to be written, not {@code null}.
     * @param flush  If {@code true}, the stream will be flushed after writing the line.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
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
     * Writes the string representation of each object in an Iterator to a file. Each object is written as a single line using the default charset.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterator containing the objects to be written.
     * @param output The File where the objects' string representations are to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
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
     * Writes the string representation of each object in an Iterator to an OutputStream. Each object is written as a single line using the default charset.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterator containing the objects to be written.
     * @param output The OutputStream where the objects' string representations are to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterator<?> lines, final OutputStream output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     * Writes the string representation of each object in an Iterator to an OutputStream. Each object is written as a single line using the default charset.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterator containing the objects to be written.
     * @param output The OutputStream where the objects' string representations are to be written, not {@code null}.
     * @param flush  If {@code true}, the stream will be flushed after writing the lines.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterator<?> lines, final OutputStream output, final boolean flush) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, IOUtil.newOutputStreamWriter(output), flush); // NOSONAR
    }

    /**
     * Writes the string representation of each object in an Iterator to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterator containing the objects to be written.
     * @param output The Writer where the objects' string representations are to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterator<?> lines, final Writer output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     * Writes the string representation of each object in an Iterator to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterator containing the objects to be written.
     * @param output The Writer where the objects' string representations are to be written, not {@code null}.
     * @param flush  If {@code true}, the stream will be flushed after writing the lines.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
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
     * Writes the string representation of each object in an Iterable to a file. Each object is written as a single line using the default charset.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterable containing the objects to be written.
     * @param output The File where the objects' string representations are to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
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
    //     * @throws IOException if an I/O error occurs.
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
     * Writes the string representation of each object in an Iterable to an OutputStream. Each object is written as a single line using the default charset.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterable containing the objects to be written.
     * @param output The OutputStream where the objects' string representations are to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterable<?> lines, final OutputStream output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     * Writes the string representation of each object in an Iterable to an OutputStream. Each object is written as a single line using the default charset.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterable containing the objects to be written.
     * @param output The OutputStream where the objects' string representations are to be written, not {@code null}.
     * @param flush  If {@code true}, the stream will be flushed after writing the lines.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
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
     * Writes the string representation of each object in an Iterable to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterable containing the objects to be written.
     * @param output The Writer where the objects' string representations are to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterable<?> lines, final Writer output) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     * Writes the string representation of each object in an Iterable to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  The Iterable containing the objects to be written.
     * @param output The Writer where the objects' string representations are to be written, not {@code null}.
     * @param flush  If {@code true}, the stream will be flushed after writing the lines.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
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
     * Writes the string representation of a boolean to a Writer.
     *
     * @param b      The boolean value to be written.
     * @param output The Writer where the boolean's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final boolean b, final Writer output) throws IOException {
        output.write(N.stringOf(b));
    }

    //    /**
    //     *
    //     * @param b
    //     * @param output
    //     * @param flush
    //     * @throws IOException if an I/O error occurs.
    //     */
    //    static void write(final boolean b, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(b));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     * Writes a single character to a Writer.
     *
     * @param c      The character to be written.
     * @param output The Writer where the character is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final char c, final Writer output) throws IOException {
        output.write(c);
    }

    //    /**
    //     *
    //     * @param c
    //     * @param output
    //     * @param flush
    //     * @throws IOException if an I/O error occurs.
    //     */
    //    static void write(final char c, final Writer output, final boolean flush) throws IOException {
    //        output.write(c);
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     * Writes the string representation of a byte to a Writer.
     *
     * @param b      The byte value to be written.
     * @param output The Writer where the byte's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte b, final Writer output) throws IOException {
        output.write(N.stringOf(b));
    }

    //    /**
    //     *
    //     * @param b
    //     * @param output
    //     * @param flush
    //     * @throws IOException if an I/O error occurs.
    //     */
    //    static void write(final byte b, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(b));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     * Writes the string representation of a short to a Writer.
     *
     * @param s      The short value to be written.
     * @param output The Writer where the short's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final short s, final Writer output) throws IOException {
        output.write(N.stringOf(s));
    }

    //    /**
    //     *
    //     * @param s
    //     * @param output
    //     * @param flush
    //     * @throws IOException if an I/O error occurs.
    //     */
    //    static void write(final short s, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(s));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     * Writes the string representation of an integer to a Writer.
     *
     * @param i      The integer value to be written.
     * @param output The Writer where the integer's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final int i, final Writer output) throws IOException {
        output.write(N.stringOf(i));
    }

    //    /**
    //     *
    //     * @param i
    //     * @param output
    //     * @param flush
    //     * @throws IOException if an I/O error occurs.
    //     */
    //    static void write(final int i, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(i));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     * Writes the string representation of a long to a Writer.
     *
     * @param lng    The long value to be written.
     * @param output The Writer where the long's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final long lng, final Writer output) throws IOException {
        output.write(N.stringOf(lng));
    }

    //    /**
    //     *
    //     * @param lng
    //     * @param output
    //     * @param flush
    //     * @throws IOException if an I/O error occurs.
    //     */
    //    static void write(final long lng, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(lng));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     * Writes the string representation of a float to a Writer.
     *
     * @param f    The float value to be written.
     * @param output The Writer where the float string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final float f, final Writer output) throws IOException {
        output.write(N.stringOf(f));
    }

    //    /**
    //     *
    //     * @param f
    //     * @param output
    //     * @param flush
    //     * @throws IOException if an I/O error occurs.
    //     */
    //    static void write(final float f, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(f));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     * Writes the string representation of a double to a Writer.
     *
     * @param d    The double value to be written.
     * @param output The Writer where the double string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final double d, final Writer output) throws IOException {
        output.write(N.stringOf(d));
    }

    //    /**
    //     *
    //     * @param d
    //     * @param output
    //     * @param flush
    //     * @throws IOException if an I/O error occurs.
    //     */
    //    static void write(final double d, final Writer output, final boolean flush) throws IOException {
    //        output.write(N.stringOf(d));
    //
    //        if (flush) {
    //            output.flush();
    //        }
    //    }

    /**
     * Writes the string representation of an object to a Writer.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     * Note: This method also protects write(boolean/char/byte/.../double, Writer) from NullPointerException.
     *
     * @param obj    The object whose string representation is to be written.
     * @param output The Writer where the object's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void write(final Object obj, final Writer output) throws IOException { // Note: DO NOT remove/update this method because it also protects write(boolean/char/byte/.../double, Writer) from NullPointerException.
        output.write(N.toString(obj));
    }

    /**
     * Writes the byte array representation of a CharSequence to a File.
     *
     * @param cs     The CharSequence whose byte array representation is to be written.
     * @param output The File where the CharSequence's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes()
     */
    public static void write(final CharSequence cs, final File output) throws IOException {
        write(cs, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the byte array representation of a CharSequence to a File using the specified Charset.
     *
     * @param cs      The CharSequence whose byte array representation is to be written.
     * @param charset The Charset to be used to encode the CharSequence into a sequence of bytes.
     * @param output  The File where the CharSequence's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes(Charset)
     */
    public static void write(final CharSequence cs, Charset charset, final File output) throws IOException {
        charset = checkCharset(charset);

        write(toByteArray(cs, charset), output);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream.
     *
     * @param cs     The CharSequence whose byte array representation is to be written.
     * @param output The OutputStream where the CharSequence's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes()
     */
    public static void write(final CharSequence cs, final OutputStream output) throws IOException {
        write(cs, output, false);
    }

    /**
     * Writes the byte array representation of a CharSequence to a OutputStream using the specified Charset.
     *
     * @param cs      The CharSequence whose byte array representation is to be written.
     * @param charset The Charset to be used to encode the CharSequence into a sequence of bytes.
     * @param output  The File where the CharSequence's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes(Charset)
     */
    public static void write(final CharSequence cs, final Charset charset, final OutputStream output) throws IOException {
        write(cs, charset, output, false);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream.
     *
     * @param cs     The CharSequence whose byte array representation is to be written.
     * @param output The OutputStream where the CharSequence's byte array representation is to be written, not {@code null}.
     * @param flush  If {@code true}, the output stream is flushed after writing the CharSequence.
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes()
     */
    public static void write(final CharSequence cs, final OutputStream output, final boolean flush) throws IOException {
        write(cs, DEFAULT_CHARSET, output, flush);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream using the specified Charset.
     *
     * @param cs      The CharSequence whose byte array representation is to be written.
     * @param charset The Charset to be used to encode the CharSequence into a sequence of bytes.
     * @param output  The OutputStream where the CharSequence's byte array representation is to be written, not {@code null}.
     * @param flush   If {@code true}, the output stream is flushed after writing the CharSequence.
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes(Charset)
     */
    public static void write(final CharSequence cs, Charset charset, final OutputStream output, final boolean flush) throws IOException {
        charset = checkCharset(charset);

        output.write(N.toString(cs).getBytes(charset));

        if (flush) {
            output.flush();
        }
    }

    /**
     * Writes the string representation of a CharSequence to a Writer.
     *
     * @param cs     The CharSequence whose string representation is to be written.
     * @param output The Writer where the CharSequence's string representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final CharSequence cs, final Writer output) throws IOException {
        write(cs, output, false);
    }

    /**
     * Writes the string representation of a CharSequence to a Writer.
     *
     * @param cs     The CharSequence whose string representation is to be written.
     * @param output The Writer where the CharSequence's string representation is to be written, not {@code null}.
     * @param flush  If {@code true}, the Writer is flushed after writing the CharSequence.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final CharSequence cs, final Writer output, final boolean flush) throws IOException {
        output.append(cs);

        if (flush) {
            output.flush();
        }
    }

    /**
     * Writes the byte array representation of a character array to a File.
     *
     * @param chars  The character array whose byte array representation is to be written.
     * @param output The File where the character array's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void write(final char[] chars, final File output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     * Writes the byte array representation of a character array to a File using the specified Charset.
     *
     * @param chars   The character array whose byte array representation is to be written.
     * @param charset The Charset to be used to encode the character array into a sequence of bytes.
     * @param output  The File where the character array's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void write(final char[] chars, final Charset charset, final File output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, charset, output);
    }

    /**
     * Writes the byte array representation of a character array to a File.
     *
     * @param chars  The character array whose byte array representation is to be written.
     * @param offset The starting position in the character array.
     * @param count  The number of characters to be written from the character array.
     * @param output The File where the character array's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final File output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the byte array representation of a character array to a File using the specified Charset.
     *
     * @param chars   The character array whose byte array representation is to be written.
     * @param offset  The starting position in the character array.
     * @param count   The number of characters to be written from the character array.
     * @param charset The Charset to be used to encode the character array into a sequence of bytes.
     * @param output  The File where the character array's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final File output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars2Bytes(chars, offset, count, charset), output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream.
     *
     * @param chars  The character array whose byte array representation is to be written.
     * @param output The OutputStream where the character array's byte array representation is to be written.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final char[] chars, final OutputStream output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the specified Charset.
     *
     * @param chars   The character array whose byte array representation is to be written.
     * @param charset The Charset to be used to encode the character array into a sequence of bytes.
     * @param output  The OutputStream where the character array's byte array representation is to be written.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final char[] chars, final Charset charset, final OutputStream output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, charset, output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream.
     *
     * @param chars  The character array whose byte array representation is to be written.
     * @param offset The starting position in the character array.
     * @param count  The number of characters to be written from the character array.
     * @param output The OutputStream where the character array's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final OutputStream output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the specified Charset.
     *
     * @param chars   The character array whose byte array representation is to be written.
     * @param offset  The starting position in the character array.
     * @param count   The number of characters to be written from the character array.
     * @param charset The Charset to be used to encode the character array into a sequence of bytes.
     * @param output  The OutputStream where the character array's byte array representation is to be written, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final OutputStream output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, charset, output, false);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream.
     *
     * @param chars  The character array whose byte array representation is to be written.
     * @param output The OutputStream where the character array's byte array representation is to be written, not {@code null}.
     * @param flush  If {@code true}, the output stream is flushed after writing the CharSequence.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void write(final char[] chars, final OutputStream output, final boolean flush) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output, flush);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream.
     *
     * @param chars  The character array whose byte array representation is to be written.
     * @param offset The starting position in the character array.
     * @param count  The number of characters to be written from the character array.
     * @param output The OutputStream where the character array's byte array representation is to be written, not {@code null}.
     * @param flush  If {@code true}, the output stream is flushed after writing the CharSequence.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final OutputStream output, final boolean flush) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output, flush);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the specified Charset.
     *
     * @param chars   The character array whose byte array representation is to be written.
     * @param offset  The starting position in the character array.
     * @param count   The number of characters to be written from the character array.
     * @param charset The Charset to be used to encode the character array into a sequence of bytes.
     * @param output  The OutputStream where the character array's byte array representation is to be written, not {@code null}.
     * @param flush   If {@code true}, the output stream is flushed after writing the CharSequence.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final OutputStream output, final boolean flush)
            throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars2Bytes(chars, offset, count, charset), output, flush);
    }

    /**
     * Writes the string representation of a character array to a Writer.
     *
     * @param chars  The character array to be written.
     * @param output The Writer where the character array is to be written.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final char[] chars, final Writer output) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     * Writes a portion of a character array to a Writer.
     *
     * @param chars  The character array to be written.
     * @param offset The starting position in the character array.
     * @param count  The number of characters to be written from the character array.
     * @param output The Writer where the character array is to be written.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final char[] chars, final int offset, final int count, final Writer output) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, output, false);
    }

    /**
     * Writes an array of characters to a Writer.
     *
     * @param chars  The character array to be written.
     * @param output The Writer where the character array is to be written.
     * @param flush  If {@code true}, the output writer will be flushed after writing.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final char[] chars, final Writer output, final boolean flush) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output, flush);
    }

    /**
     * Writes a portion of a character array to a Writer.
     *
     * @param chars  The character array to be written.
     * @param offset The starting position in the character array.
     * @param count  The number of characters to be written from the character array.
     * @param output The Writer where the character array is to be written.
     * @param flush  If {@code true}, the output writer will be flushed after writing.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final char[] chars, final int offset, final int count, final Writer output, final boolean flush) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        output.write(chars, offset, count);

        if (flush) {
            output.flush();
        }
    }

    /**
     * Writes an array of bytes to a File.
     *
     * @param bytes  The byte array to be written.
     * @param output The File where the byte array is to be written.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte[] bytes, final File output) throws IOException {
        if (N.isEmpty(bytes)) {
            return;
        }

        write(bytes, 0, bytes.length, output);
    }

    /**
     * Writes a portion of a byte array to a File.
     *
     * @param bytes  The byte array to be written.
     * @param offset The starting position in the byte array.
     * @param count  The number of bytes to be written from the byte array.
     * @param output The File where the byte array is to be written.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte[] bytes, final int offset, final int count, final File output) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

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
     * Writes an array of bytes to an OutputStream.
     *
     * @param bytes  The byte array to be written.
     * @param output The OutputStream where the byte array is to be written.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte[] bytes, final OutputStream output) throws IOException {
        if (N.isEmpty(bytes)) {
            return;
        }

        write(bytes, 0, bytes.length, output);
    }

    /**
     * Writes a portion of a byte array to an OutputStream.
     *
     * @param bytes  The byte array to be written.
     * @param offset The starting position in the byte array.
     * @param count  The number of bytes to be written from the byte array.
     * @param output The OutputStream where the byte array is to be written.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte[] bytes, final int offset, final int count, final OutputStream output) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(bytes) >= offset) {
            return;
        }

        write(bytes, offset, count, output, false);
    }

    /**
     * Writes an array of bytes to an OutputStream.
     *
     * @param bytes  The byte array to be written.
     * @param output The OutputStream where the byte array is to be written.
     * @param flush  If {@code true}, the output stream is flushed after writing the byte array.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte[] bytes, final OutputStream output, final boolean flush) throws IOException {
        if (N.isEmpty(bytes)) {
            return;
        }

        write(bytes, 0, bytes.length, output, flush);
    }

    /**
     * Writes a portion of a byte array to an OutputStream.
     *
     * @param bytes  The byte array to be written.
     * @param offset The starting position in the byte array.
     * @param count  The number of bytes to be written from the byte array.
     * @param output The OutputStream where the byte array is to be written.
     * @param flush  If {@code true}, the output stream is flushed after writing the byte array.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte[] bytes, final int offset, final int count, final OutputStream output, final boolean flush) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

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
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output);
    }

    /**
     * Writes a portion of a file to another file.
     *
     * @param source The source file to be written.
     * @param offset The starting position in the source file.
     * @param count  The number of bytes to be written from the source file.
     * @param output The output file where the source file is to be written.
     * @return The number of bytes written.
     * @throws IOException if an I/O error occurs.
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
     * Writes the content of a source file to an OutputStream.
     *
     * @param source The source file to be written.
     * @param output The OutputStream where the source file is to be written.
     * @return The number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final OutputStream output) throws IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of the source file to the output stream, starting from the specified offset and writing up to the specified count.
     *
     * @param source The file to read from.
     * @param offset The position in the file to start reading from.
     * @param count  The maximum number of bytes to write to the output.
     * @param output The output stream to write to.
     * @return The total number of bytes written to the output stream.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final long offset, final long count, final OutputStream output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of a source file to an OutputStream.
     *
     * @param source The source file to be written.
     * @param output The OutputStream where the source file is to be written.
     * @param flush  If {@code true}, the output stream will be flushed after the write operation.
     * @return The number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final OutputStream output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes the content of the source file to the output stream, starting from the specified offset and writing up to the specified count.
     * If the flush parameter is {@code true}, the output stream is flushed after the write operation.
     *
     * @param source The file to read from.
     * @param offset The position in the file to start reading from.
     * @param count  The maximum number of bytes to write to the output.
     * @param output The output stream to write to.
     * @param flush  If {@code true}, the output stream is flushed after the write operation.
     * @return The total number of bytes written to the output stream.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final long offset, final long count, final OutputStream output, final boolean flush) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

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
    //     * @throws IOException if an I/O error occurs.
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
    //     * @throws IOException if an I/O error occurs.
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
    //     * @throws IOException if an I/O error occurs.
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
    //     * @throws IOException if an I/O error occurs.
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
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output);
    }

    /**
     * Writes the content of an InputStream to a File.
     *
     * @param source The InputStream to be written.
     * @param offset The starting point from where to begin writing bytes from the InputStream.
     * @param count  The maximum number of bytes to write to the File.
     * @param output The File where the InputStream is to be written.
     * @return The number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final long offset, final long count, final File output) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

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
     * Writes the content of an InputStream to an OutputStream.
     *
     * @param source The InputStream to be written.
     * @param output The OutputStream where the InputStream is to be written.
     * @return The number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final OutputStream output) throws IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of an InputStream to an OutputStream.
     *
     * @param source The InputStream to be written.
     * @param offset The starting point from where to begin writing bytes from the InputStream.
     * @param count  The maximum number of bytes to write to the OutputStream.
     * @param output The OutputStream where the InputStream is to be written.
     * @return The number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of an InputStream to an OutputStream.
     *
     * @param source The InputStream to be written.
     * @param output The OutputStream where the InputStream is to be written.
     * @param flush  If {@code true}, the output stream will be flushed after writing.
     * @return The number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final OutputStream output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes the content of the input stream to the output stream, starting from the specified offset and writing up to the specified count.
     * If the flush parameter is {@code true}, the output stream is flushed after the write operation.
     *
     * @param source The input stream to read from.
     * @param offset The position in the input stream to start reading from.
     * @param count  The maximum number of bytes to write to the output.
     * @param output The output stream to write to.
     * @param flush  If {@code true}, the output stream is flushed after the write operation.
     * @return The total number of bytes written to the output stream.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

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
     * Writes the content of a Reader to a File.
     *
     * @param source The Reader to be written.
     * @param output The File where the Reader's content is to be written.
     * @return The number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final File output) throws IOException {
        return write(source, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the content of a Reader to a File using the specified Charset.
     *
     * @param source  The Reader to be written.
     * @param charset The Charset to be used to open the specified file for writing.
     * @param output  The File where the Reader's content is to be written.
     * @return The number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final Charset charset, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, charset, output);
    }

    /**
     * Writes the content of a Reader to a File starting from a specific offset and up to a certain count.
     *
     * @param source The Reader to be written.
     * @param offset The position in the Reader to start writing from.
     * @param count  The maximum number of characters to be written.
     * @param output The File where the Reader's content is to be written.
     * @return The number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final long offset, final long count, final File output) throws IOException {
        return write(source, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the content of a Reader to a File starting from a specific offset and up to a certain count using the specified Charset.
     *
     * @param source  The Reader to be written.
     * @param offset  The position in the Reader to start writing from.
     * @param count   The maximum number of characters to be written.
     * @param charset The Charset to be used to open the specified file for writing.
     * @param output  The File where the Reader's content is to be written.
     * @return The number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final long offset, final long count, final Charset charset, final File output) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        Writer writer = null;

        try { //NOSONAR
            writer = IOUtil.newFileWriter(output, checkCharset(charset));

            final long result = write(source, offset, count, writer);

            writer.flush();

            return result;
        } finally {
            close(writer);
        }
    }

    /**
     * Writes the content from a Reader to a Writer.
     *
     * @param source The Reader to be written.
     * @param output The Writer where the Reader's content is to be written.
     * @return The number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final Writer output) throws IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of a Reader to a Writer starting from a specific offset and up to a certain count.
     *
     * @param source The Reader to be written.
     * @param offset The position in the Reader to start writing from.
     * @param count  The maximum number of characters to be written.
     * @param output The Writer where the Reader's content is to be written.
     * @return The number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final long offset, final long count, final Writer output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of a Reader to a Writer.
     *
     * @param source The Reader to be written.
     * @param output The Writer where the Reader's content is to be written.
     * @param flush  If {@code true}, the output Writer is flushed after writing.
     * @return The number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final Writer output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes a specified number of characters from a Reader to a Writer, starting from a specified offset.
     *
     * @param source The Reader to read from.
     * @param offset The position in the Reader to start reading from.
     * @param count  The number of characters to read from the Reader and write to the Writer.
     * @param output The Writer to write to.
     * @param flush  If {@code true}, the output Writer is flushed after writing.
     * @return The total number of characters written to the Writer.
     * @throws IllegalArgumentException If the source Reader is {@code null} or if the count is negative.
     * @throws IOException              if an I/O error occurs.
     */
    public static long write(final Reader source, final long offset, final long count, final Writer output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);

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
     * @param bytes      The byte array to append to the file.
     * @param targetFile The file to which the byte array will be appended.
     * @throws IOException if an I/O error occurs.
     */
    public static void append(final byte[] bytes, final File targetFile) throws IOException {
        if (N.isEmpty(bytes)) {
            return;
        }

        append(bytes, 0, bytes.length, targetFile);
    }

    /**
     * Appends the given byte array to the specified file starting from the specified offset up to the count.
     *
     * @param bytes      The byte array to append to the file.
     * @param offset     The starting index from where to append the bytes.
     * @param count      The number of bytes to append from the byte array.
     * @param targetFile The file to which the byte array will be appended.
     * @throws IOException if an I/O error occurs.
     */
    public static void append(final byte[] bytes, final int offset, final int count, final File targetFile) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

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
     * Appends the given character array to the specified file.
     *
     * @param chars      The character array to append to the file.
     * @param targetFile The file to which the character array will be appended.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void append(final char[] chars, final File targetFile) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        append(chars, 0, chars.length, targetFile);
    }

    /**
     * Appends the given character array to the specified file.
     *
     * @param chars      The character array to append to the file.
     * @param charset    The Charset to be used to encode the character array into a sequence of bytes.
     * @param targetFile The file to which the character array will be appended.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void append(final char[] chars, final Charset charset, final File targetFile) throws IOException {
        if (N.isEmpty(chars)) {
            return;
        }

        append(chars, 0, chars.length, charset, targetFile);
    }

    /**
     * Appends the content of the character array to the target file.
     * The content to be appended starts at the specified offset and extends count characters.
     *
     * @param chars      The character array to append to the file.
     * @param offset     The initial offset in the character array.
     * @param count      The number of characters to append.
     * @param targetFile The file to which the character array will be appended, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void append(final char[] chars, final int offset, final int count, final File targetFile) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        append(chars, offset, count, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of the character array to the target file.
     * The content to be appended starts at the specified offset and extends count characters.
     *
     * @param chars      The character array to append to the file.
     * @param offset     The initial offset in the character array.
     * @param count      The number of characters to append.
     * @param charset    The Charset to be used to encode the character array into a sequence of bytes.
     * @param targetFile The file to which the character array will be appended, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void append(final char[] chars, final int offset, final int count, final Charset charset, final File targetFile) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        append(chars2Bytes(chars, offset, count, charset), targetFile);
    }

    /**
     * Appends the content of the CharSequence to the target file.
     *
     * @param cs         The CharSequence to append to the file.
     * @param targetFile The file to which the CharSequence will be appended, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void append(final CharSequence cs, final File targetFile) throws IOException {
        append(cs, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of the CharSequence to the target file.
     *
     * @param cs         The CharSequence to append to the file.
     * @param charset    The Charset to be used to encode the character array into a sequence of bytes.
     * @param targetFile The file to which the CharSequence will be appended, not {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void append(final CharSequence cs, final Charset charset, final File targetFile) throws IOException {
        append(toByteArray(cs, charset), targetFile);
    }

    /**
     * Appends the content of the source file to the target file.
     *
     * @param source     The source file to read from, not {@code null}.
     * @param targetFile The target file to append to, not {@code null}.
     * @return The number of bytes appended.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final File source, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, targetFile);
    }

    /**
     * Appends the content of the source file to the target file.
     * The content to be appended is read from the source file starting from the specified offset in bytes and up to the specified count.
     *
     * @param source     The source file to read from, not {@code null}.
     * @param offset     The starting point in bytes from where to read in the source file.
     * @param count      The maximum number of bytes to read from the source file.
     * @param targetFile The file to which the content will be appended, not {@code null}.
     * @return The number of bytes appended to the target file.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final File source, final long offset, final long count, final File targetFile) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

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
     * Appends the content of the InputStream to the target file.
     * The content to be appended is read from the InputStream.
     *
     * @param source     The InputStream to read from, not {@code null}.
     * @param targetFile The file to which the InputStream content will be appended, not {@code null}.
     * @return The number of bytes appended to the target file.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final InputStream source, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, targetFile);
    }

    /**
     * Appends the content of the InputStream to the target file.
     * The content to be appended is read from the InputStream starting from the specified offset in bytes and up to the specified count.
     *
     * @param source     The InputStream to read from, not {@code null}.
     * @param offset     The starting point in bytes from where to read in the InputStream.
     * @param count      The maximum number of bytes to read from the InputStream.
     * @param targetFile The file to which the InputStream content will be appended, not {@code null}.
     * @return The number of bytes appended to the target file.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final InputStream source, final long offset, final long count, final File targetFile) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        OutputStream output = null;

        try { //NOSONAR
            createNewFileIfNotExists(targetFile);

            output = newFileOutputStream(targetFile, true);

            return write(source, offset, count, output, true);
        } finally {
            close(output);
        }
    }

    /**
     * Appends the content of the Reader to the target file.
     * This file is opened to write with the default charset.
     *
     * @param source     The Reader to read from, not {@code null}.
     * @param targetFile The file to which the Reader content will be appended, not {@code null}.
     * @return The number of characters appended to the target file.
     * @throws IOException if an I/O error occurs.
     */

    public static long append(final Reader source, final File targetFile) throws IOException {
        return append(source, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of the Reader to the target file.
     * This file is opened to write with the provided charset.
     *
     * @param source     The Reader to read from, not {@code null}.
     * @param charset    The Charset to be used to open the specified for writing.
     * @param targetFile The file to which the Reader content will be appended, not {@code null}.
     * @return The number of characters appended to the target file.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final Reader source, final Charset charset, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, charset, targetFile);
    }

    /**
     * Appends the content of the Reader to the target file.
     * The content to be appended is read from the Reader starting from the specified offset in characters and up to the specified count.
     * This file is opened to write with the default charset.
     *
     * @param source     The Reader to read from, not {@code null}.
     * @param offset     The position in the Reader to start reading from.
     * @param count      The maximum number of characters to read from the Reader.
     * @param targetFile The file to which the Reader content will be appended, not {@code null}.
     * @return The number of characters appended to the target file.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final Reader source, final long offset, final long count, final File targetFile) throws IOException {
        return append(source, offset, count, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of the Reader to the target file.
     * The content to be appended is read from the Reader starting from the specified offset in characters and up to the specified count.
     * This file is opened to write with the provided charset.
     *
     * @param source     The Reader to read from, not {@code null}.
     * @param offset     The position in the Reader to start reading from.
     * @param count      The maximum number of characters to read from the Reader.
     * @param charset    The Charset to be used to open the specified for writing.
     * @param targetFile The file to which the Reader content will be appended, not {@code null}.
     * @return The number of characters appended to the target file.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final Reader source, final long offset, final long count, final Charset charset, final File targetFile) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        Writer writer = null;

        try { //NOSONAR
            createNewFileIfNotExists(targetFile);

            writer = IOUtil.newFileWriter(targetFile, checkCharset(charset), true);

            final long result = write(source, offset, count, writer);

            writer.flush();

            return result;
        } finally {
            close(writer);
        }
    }

    /**
     * Appends the string representation of the specified object as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     *
     * @param obj        The object whose string representation is to be appended to the file.
     * @param targetFile The file to which the object's string representation will be appended, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #writeLine(Object, File)
     * @see N#toString(Object)
     */
    public static void appendLine(final Object obj, final File targetFile) throws IOException {
        appendLine(obj, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the string representation of the specified object as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     *
     * @param charset    The Charset to be used to encode string representation of the specified object into a sequence of bytes.
     * @param targetFile The file to which the object's string representation will be appended, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #writeLine(Object, File)
     * @see N#toString(Object)
     */
    public static void appendLine(final Object obj, final Charset charset, final File targetFile) throws IOException {
        final String str = N.toString(obj) + IOUtil.LINE_SEPARATOR;

        append(toByteArray(str, charset), targetFile);
    }

    /**
     * Appends the string representation of each object in the provided iterable as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     * This file is opened to write with the default charset.
     *
     * @param lines      The iterable whose elements' string representations are to be appended to the file.
     * @param targetFile The file to which the elements' string representations will be appended, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #writeLines(Iterable, File)
     * @see N#toString(Object)
     */
    public static void appendLines(final Iterable<?> lines, final File targetFile) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        appendLines(lines, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the string representation of each object in the provided iterable as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     * This file is opened to write with the provided charset.
     *
     * @param lines      The iterable whose elements' string representations are to be appended to the file.
     * @param charset    The Charset to be used to open the specified file for writing.
     * @param targetFile The file to which the elements' string representations will be appended, not {@code null}.
     * @throws IOException if an I/O error occurs.
     * @see #writeLines(Iterable, File)
     * @see N#toString(Object)
     */
    public static void appendLines(final Iterable<?> lines, final Charset charset, final File targetFile) throws IOException {
        if (N.isEmpty(lines)) {
            return;
        }

        Writer writer = null;

        try { //NOSONAR
            createNewFileIfNotExists(targetFile);

            writer = IOUtil.newFileWriter(targetFile, checkCharset(charset), true);

            writeLines(lines, writer, true);
        } finally {
            close(writer);
        }
    }

    /**
     * Transfers bytes from a source channel to a target channel.
     *
     * @param src    The source channel from which bytes are to be read.
     * @param output The target channel to which bytes are to be written.
     * @return The number of bytes transferred.
     * @throws IOException if an I/O error occurs during the transfer.
     */
    public static long transfer(final ReadableByteChannel src, final WritableByteChannel output) throws IOException {
        N.checkArgNotNull(src, cs.ReadableByteChannel);
        N.checkArgNotNull(output, cs.WritableByteChannel);

        return write(Channels.newInputStream(src), Channels.newOutputStream(output), true);
    }

    /**
     * Skips over and discards a specified number of bytes from the input stream.
     *
     * @param input  The InputStream from which bytes are to be skipped.
     * @param toSkip The number of bytes to be skipped.
     * @return The actual number of bytes skipped.
     * @throws If an I/O error occurs, including if the InputStream reaches the end before skipping all the bytes.
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
     * @param input  The Reader from which characters are to be skipped.
     * @param toSkip The number of characters to be skipped.
     * @return The actual number of characters skipped.
     * @throws If an I/O error occurs, including if the Reader reaches the end before skipping all the characters.
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
     * @param input  The input stream to be skipped.
     * @param toSkip The number of bytes to be skipped.
     * @throws If an I/O error occurs, including if the input stream reaches the end before skipping all the bytes.
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
     * @param input  The Reader from which characters are to be skipped.
     * @param toSkip The number of characters to be skipped.
     * @throws If an I/O error occurs, including if the Reader reaches the end before skipping all the characters.
     */
    public static void skipFully(final Reader input, final long toSkip) throws IOException {
        final long skipped = skip(input, toSkip);

        if (skipped != toSkip) {
            throw new IOException("Chars to skip: " + toSkip + " actual: " + skipped);
        }
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     * <br />
     * <p>
     * Maps a file into memory, creating a MappedByteBuffer that represents the file's content.
     *
     * @param file The file to be mapped into memory.
     * @return A MappedByteBuffer that represents the content of the file.
     * @throws IllegalArgumentException if the provided file is {@code null}.
     * @throws IOException              if an I/O error occurs during the operation.
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);

        return map(file, MapMode.READ_ONLY);
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     * <br />
     * <p>
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
     * @throws IOException              if an I/O error occurs.
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file, final MapMode mode) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(mode, cs.mode);

        if (!file.exists()) {
            throw new IllegalArgumentException(file.getName() + " is not found");
        }

        return map(file, mode, 0, file.length());
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     * <br />
     * <p>
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
     * @param file   the file to map
     * @param mode   the mode to use when mapping {@code file}
     * @param offset
     * @param count
     * @return a buffer reflecting {@code file}
     * @throws IllegalArgumentException
     * @throws IOException              if an I/O error occurs.
     * @see FileChannel#map(MapMode, long, long)
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
     * <br />
     * <p>
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
            if (component.isEmpty() || component.equals(".")) {
                //NOSONAR
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
        } else if (result.isEmpty()) {
            result = ".";
        }

        return result;
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     * <br />
     * <p>
     * Returns the <a href="http://en.wikipedia.org/wiki/Filename_extension">file
     * extension</a> for the given file name, or the empty string if the file has
     * no extension.  The result does not include the '{@code .}'.
     *
     * @param fullName
     * @return
     * @throws IllegalArgumentException
     */
    public static String getFileExtension(final String fullName) throws IllegalArgumentException {
        N.checkArgNotNull(fullName);

        final String fileName = new File(fullName).getName();
        final int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     * <br />
     * <p>
     * Returns the file name without its
     * <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
     * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
     *
     * @param file The name of the file to trim the extension from. This can be either a fully
     *             qualified file name (including a path) or just a file name.
     * @return The file name without its path or extension.
     * @throws IllegalArgumentException
     */
    public static String getNameWithoutExtension(final String file) throws IllegalArgumentException {
        N.checkArgNotNull(file);

        final String fileName = new File(file).getName();
        final int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    /**
     * Creates a new AppendableWriter instance that wraps the provided Appendable object.
     *
     * @param appendable The Appendable object to be wrapped by the AppendableWriter.
     * @return A new instance of AppendableWriter that wraps the provided Appendable.
     */
    public static AppendableWriter newAppendableWriter(final Appendable appendable) {
        return new AppendableWriter(appendable);
    }

    /**
     * Creates a new StringWriter instance.
     *
     * @return A new instance of StringWriter.
     */
    public static StringWriter newStringWriter() {
        return new StringWriter();
    }

    /**
     * Creates a new StringWriter instance with the specified initial size.
     *
     * @param initialSize The initial size of the buffer.
     * @return A new instance of StringWriter with the specified initial size.
     */
    public static StringWriter newStringWriter(final int initialSize) {
        return new StringWriter(initialSize);
    }

    /**
     * Creates a new StringWriter instance with the content of the specified StringBuilder.
     *
     * @param sb The StringBuilder whose content is to be used for the StringWriter.
     * @return A new instance of StringWriter with the content of the specified StringBuilder.
     */
    public static StringWriter newStringWriter(final StringBuilder sb) {
        return new StringWriter(sb);
    }

    /**
     * Creates a new ByteArrayOutputStream instance.
     *
     * @return A new instance of ByteArrayOutputStream.
     */
    public static ByteArrayOutputStream newByteArrayOutputStream() {
        return new ByteArrayOutputStream();
    }

    /**
     * Creates a new ByteArrayOutputStream instance with the specified initial capacity.
     *
     * @param initCapacity The initial capacity of the ByteArrayOutputStream.
     * @return A new instance of ByteArrayOutputStream with the specified initial capacity.
     */
    public static ByteArrayOutputStream newByteArrayOutputStream(final int initCapacity) {
        return new ByteArrayOutputStream(initCapacity);
    }

    /**
     * Creates a new FileInputStream instance for the specified file.
     *
     * @param file The file to be opened for reading.
     * @return A new FileInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileInputStream#FileInputStream(File)
     */
    public static FileInputStream newFileInputStream(final File file) throws UncheckedIOException {
        try {
            return new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileInputStream instance for the specified file name.
     *
     * @param name The name of the file to be opened for reading.
     * @return A new FileInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newFileInputStream(File)
     * @see FileInputStream#FileInputStream(String)
     */
    public static FileInputStream newFileInputStream(final String name) throws UncheckedIOException {
        try {
            return new FileInputStream(name);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileOutputStream instance for the specified file.
     *
     * @param file The file to be opened for writing.
     * @return A new FileOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileOutputStream#FileOutputStream(File)
     */
    public static FileOutputStream newFileOutputStream(final File file) throws UncheckedIOException {
        try {
            return new FileOutputStream(file);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileOutputStream instance for the specified file.
     *
     * @param file   The file to be opened for writing.
     * @param append {@code true} if the file is to be opened for appending.
     * @return A new FileOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileOutputStream#FileOutputStream(File, boolean)
     */
    public static FileOutputStream newFileOutputStream(final File file, final boolean append) throws UncheckedIOException {
        try {
            return new FileOutputStream(file, append);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileOutputStream instance for the specified file name.
     *
     * @param name The name of the file to be opened for writing.
     * @return A new FileOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileOutputStream#FileOutputStream(String)
     */
    public static FileOutputStream newFileOutputStream(final String name) throws UncheckedIOException {
        try {
            return new FileOutputStream(name);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileReader instance for the specified file and the default Charset.
     *
     * @param file The file to be opened for reading.
     * @return A new FileReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileReader#FileReader(File, Charset)
     */
    public static FileReader newFileReader(final File file) throws UncheckedIOException {
        try {
            return new FileReader(file, DEFAULT_CHARSET); // NOSONAR
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileReader instance for the specified file and charset.
     *
     * @param file    The file to be opened for reading.
     * @param charset The Charset to be used for creating the FileReader.
     * @return A new FileReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileReader#FileReader(File, Charset)
     */
    public static FileReader newFileReader(final File file, final Charset charset) throws UncheckedIOException {
        try {
            return new FileReader(file, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileWriter instance for the specified file and the default Charset.
     *
     * @param file The file to be opened for writing.
     * @return A new FileWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileWriter#FileWriter(File, Charset)
     */
    public static FileWriter newFileWriter(final File file) throws UncheckedIOException {
        try {
            return new FileWriter(file, DEFAULT_CHARSET); // NOSONAR
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileWriter instance for the specified file and charset.
     *
     * @param file    The file to be opened for writing.
     * @param charset The Charset to be used for creating the FileWriter.
     * @return A new FileWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileWriter#FileWriter(File, Charset)
     */
    public static FileWriter newFileWriter(final File file, final Charset charset) throws UncheckedIOException {
        try {
            return new FileWriter(file, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileWriter instance for the specified file and charset.
     *
     * @param file    The file to be opened for writing.
     * @param charset The Charset to be used for creating the FileWriter.
     * @param append  {@code true} if the file is to be opened for appending.
     * @return A new FileWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileWriter#FileWriter(File, Charset, boolean)
     */
    public static FileWriter newFileWriter(final File file, final Charset charset, final boolean append) throws UncheckedIOException {
        try {
            return new FileWriter(file, charset, append);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new InputStreamReader instance for the specified InputStream and the default Charset.
     *
     * @param is The InputStream to be read.
     * @return A new InputStreamReader instance.
     * @see InputStreamReader#InputStreamReader(InputStream, Charset)
     */
    public static InputStreamReader newInputStreamReader(final InputStream is) {
        return new InputStreamReader(is, DEFAULT_CHARSET); // NOSONAR
    }

    /**
     * Creates a new InputStreamReader instance for the specified InputStream and Charset.
     *
     * @param is      The InputStream to be read.
     * @param charset The Charset to be used for creating the InputStreamReader.
     * @return A new InputStreamReader instance.
     * @see InputStreamReader#InputStreamReader(InputStream, Charset)
     */
    public static InputStreamReader newInputStreamReader(final InputStream is, final Charset charset) {
        return new InputStreamReader(is, checkCharset(charset)); // NOSONAR
    }

    /**
     * Creates a new OutputStreamWriter instance for the specified OutputStream and the default Charset.
     *
     * @param os The OutputStream to be written to.
     * @return A new OutputStreamWriter instance.
     * @see OutputStreamWriter#OutputStreamWriter(OutputStream, Charset)
     */
    public static OutputStreamWriter newOutputStreamWriter(final OutputStream os) {
        return new OutputStreamWriter(os, DEFAULT_CHARSET); // NOSONAR
    }

    /**
     * Creates a new OutputStreamWriter instance for the specified OutputStream and Charset.
     *
     * @param os      The OutputStream to be written to.
     * @param charset The Charset to be used for creating the OutputStreamWriter.
     * @return A new OutputStreamWriter instance.
     * @see OutputStreamWriter#OutputStreamWriter(OutputStream, Charset)
     */
    public static OutputStreamWriter newOutputStreamWriter(final OutputStream os, final Charset charset) {
        return new OutputStreamWriter(os, checkCharset(charset));
    }

    /**
     * Creates a new BufferedReader instance for the specified file path.
     *
     * @param filePath The path of the file to be read.
     * @return A new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newBufferedReader(File)
     */
    static java.io.BufferedReader newBufferedReader(final String filePath) throws UncheckedIOException {
        return newBufferedReader(new File(filePath));
    }

    /**
     * Creates a new BufferedInputStream instance for the specified file.
     *
     * @param file The file to be read.
     * @return A new BufferedInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newFileInputStream(File)
     * @see BufferedInputStream#BufferedInputStream(InputStream)
     */
    public static BufferedInputStream newBufferedInputStream(final File file) throws UncheckedIOException {
        return new BufferedInputStream(newFileInputStream(file));
    }

    /**
     * Creates a new BufferedInputStream instance for the specified file with a specified buffer size.
     *
     * @param file The file to be read.
     * @param size The size of the buffer to be used.
     * @return A new BufferedInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newFileInputStream(File)
     * @see BufferedInputStream#BufferedInputStream(InputStream, int)
     */
    public static BufferedInputStream newBufferedInputStream(final File file, final int size) throws UncheckedIOException {
        return new BufferedInputStream(newFileInputStream(file), size);
    }

    /**
     * Creates a new BufferedOutputStream instance for the specified file.
     *
     * @param file The file to be written to.
     * @return A new BufferedOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newFileOutputStream(File)
     * @see BufferedOutputStream#BufferedOutputStream(OutputStream)
     */
    public static BufferedOutputStream newBufferedOutputStream(final File file) throws UncheckedIOException {
        return new BufferedOutputStream(newFileOutputStream(file));
    }

    /**
     * Creates a new BufferedOutputStream instance for the specified file with a specified buffer size.
     *
     * @param file The file to be written to.
     * @param size The size of the buffer to be used.
     * @return A new BufferedOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newFileOutputStream(File)
     * @see BufferedOutputStream#BufferedOutputStream(OutputStream, int)
     */
    public static BufferedOutputStream newBufferedOutputStream(final File file, final int size) throws UncheckedIOException {
        return new BufferedOutputStream(newFileOutputStream(file), size);
    }

    /**
     * Creates a new BufferedReader instance for the specified file with the default charset.
     *
     * @param file The file to be read from.
     * @return A new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newFileReader(File)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final File file) throws UncheckedIOException {
        return new java.io.BufferedReader(newFileReader(file));
    }

    /**
     * Creates a new BufferedReader instance for the specified file with a specified charset.
     *
     * @param file    The file to be read from.
     * @param charset The charset to be used.
     * @return A new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newInputStreamReader(InputStream, Charset)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final File file, final Charset charset) throws UncheckedIOException {
        return new java.io.BufferedReader(newFileReader(file, checkCharset(charset)));
    }

    /**
     * Creates a new BufferedReader instance for the specified path with the default charset.
     *
     * @param path The path of the file to be read from.
     * @return A new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see java.nio.file.Files#newBufferedReader(Path, Charset)
     */
    public static java.io.BufferedReader newBufferedReader(final Path path) throws UncheckedIOException {
        try {
            return Files.newBufferedReader(path, DEFAULT_CHARSET);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new BufferedReader instance for the specified path with a specified charset.
     *
     * @param path    The path of the file to be read from.
     * @param charset The charset to be used.
     * @return A new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see java.nio.file.Files#newBufferedReader(Path, Charset)
     */
    public static java.io.BufferedReader newBufferedReader(final Path path, final Charset charset) throws UncheckedIOException {
        try {
            return Files.newBufferedReader(path, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new BufferedReader instance for the specified InputStream with the default charset.
     *
     * @param is The InputStream to be read from.
     * @return A new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newInputStreamReader(InputStream)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final InputStream is) throws UncheckedIOException {
        return new java.io.BufferedReader(newInputStreamReader(is, DEFAULT_CHARSET)); // NOSONAR
    }

    /**
     * Creates a new BufferedReader instance for the specified InputStream with a specified charset.
     *
     * @param is      The InputStream to be read from.
     * @param charset The charset to be used.
     * @return A new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newInputStreamReader(InputStream, Charset)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final InputStream is, final Charset charset) throws UncheckedIOException {
        return new java.io.BufferedReader(newInputStreamReader(is, checkCharset(charset)));
    }

    /**
     * Creates a new BufferedWriter instance for the specified file path and default charset.
     *
     * @param filePath The path of the file to be written to.
     * @return A new BufferedWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    static java.io.BufferedWriter newBufferedWriter(final String filePath) throws UncheckedIOException {
        return newBufferedWriter(new File(filePath));
    }

    /**
     * Creates a new BufferedWriter instance for the specified file and default charset.
     *
     * @param file The file to be written to.
     * @return A new BufferedWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static java.io.BufferedWriter newBufferedWriter(final File file) throws UncheckedIOException {
        return new java.io.BufferedWriter(newFileWriter(file));
    }

    /**
     * Creates a new BufferedWriter instance for the specified file and charset.
     *
     * @param file    The file to be written to.
     * @param charset The charset to be used for writing to the file.
     * @return A new BufferedWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final File file, final Charset charset) throws UncheckedIOException {
        return new java.io.BufferedWriter(newFileWriter(file, checkCharset(charset)));
    }

    /**
     * Creates a new BufferedWriter instance for the specified OutputStream with default charset.
     *
     * @param os The OutputStream to be written to.
     * @return A new BufferedWriter instance.
     * @see #newOutputStreamWriter(OutputStream)
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final OutputStream os) {
        return new java.io.BufferedWriter(newOutputStreamWriter(os)); // NOSONAR
    }

    /**
     * Creates a new BufferedWriter instance for the specified OutputStream and Charset.
     *
     * @param os      The OutputStream to be written to.
     * @param charset The Charset to be used for writing to the OutputStream.
     * @return A new BufferedWriter instance.
     * @see #newOutputStreamWriter(OutputStream, Charset)
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final OutputStream os, final Charset charset) {
        return new java.io.BufferedWriter(newOutputStreamWriter(os, checkCharset(charset)));
    }

    /**
     * Creates a new LZ4BlockInputStream instance for the specified InputStream.
     *
     * @param is The InputStream to be read from.
     * @return A new LZ4BlockInputStream instance.
     */
    public static LZ4BlockInputStream newLZ4BlockInputStream(final InputStream is) {
        return new LZ4BlockInputStream(is);
    }

    /**
     * Creates a new LZ4BlockOutputStream instance for the specified OutputStream.
     *
     * @param os The OutputStream to be written to.
     * @return A new LZ4BlockOutputStream instance.
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os) {
        return new LZ4BlockOutputStream(os);
    }

    /**
     * Creates a new LZ4BlockOutputStream instance for the specified OutputStream with the given block size.
     *
     * @param os        The OutputStream to be written to.
     * @param blockSize The block size for the LZ4BlockOutputStream.
     * @return A new LZ4BlockOutputStream instance.
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os, final int blockSize) {
        return new LZ4BlockOutputStream(os, blockSize);
    }

    /**
     * Creates a new SnappyInputStream instance for the specified InputStream.
     *
     * @param is The InputStream to be read from.
     * @return A new SnappyInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static SnappyInputStream newSnappyInputStream(final InputStream is) throws UncheckedIOException {
        try {
            return new SnappyInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new SnappyOutputStream instance for the specified OutputStream.
     *
     * @param os The OutputStream to be written to.
     * @return A new SnappyOutputStream instance.
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os) {
        return new SnappyOutputStream(os);
    }

    /**
     * Creates a new SnappyOutputStream instance with the specified OutputStream and buffer size.
     *
     * @param os         The OutputStream to be written to.
     * @param bufferSize The size of the buffer to be used.
     * @return A new SnappyOutputStream instance.
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os, final int bufferSize) {
        return new SnappyOutputStream(os, bufferSize);
    }

    /**
     * Creates a new GZIPInputStream instance for the specified InputStream.
     *
     * @param is The InputStream to be read from.
     * @return A new GZIPInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static GZIPInputStream newGZIPInputStream(final InputStream is) throws UncheckedIOException {
        try {
            return new GZIPInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new GZIPInputStream instance with the specified InputStream and buffer size.
     *
     * @param is         The InputStream to be read from.
     * @param bufferSize The size of the buffer to be used.
     * @return A new GZIPInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static GZIPInputStream newGZIPInputStream(final InputStream is, final int bufferSize) throws UncheckedIOException {
        try {
            return new GZIPInputStream(is, bufferSize);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new GZIPOutputStream instance for the specified OutputStream.
     *
     * @param os The OutputStream to be written to.
     * @return A new GZIPOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static GZIPOutputStream newGZIPOutputStream(final OutputStream os) throws UncheckedIOException {
        try {
            return new GZIPOutputStream(os);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new GZIPOutputStream with the specified OutputStream and buffer size.
     *
     * @param os         The OutputStream to be written to.
     * @param bufferSize The size of the buffer to be used.
     * @return A new GZIPOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static GZIPOutputStream newGZIPOutputStream(final OutputStream os, final int bufferSize) throws UncheckedIOException {
        try {
            return new GZIPOutputStream(os, bufferSize);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new ZipInputStream with the specified InputStream.
     *
     * @param is The InputStream to be used for creating the ZipInputStream.
     * @return A new ZipInputStream instance.
     * @see ZipInputStream#ZipInputStream(InputStream)
     */
    public static ZipInputStream newZipInputStream(final InputStream is) {
        return new ZipInputStream(is);
    }

    /**
     * Creates a new ZipInputStream with the specified InputStream and Charset.
     *
     * @param is      The InputStream to be used for creating the ZipInputStream.
     * @param charset The Charset to be used for creating the ZipInputStream.
     * @return A new ZipInputStream instance.
     * @see ZipInputStream#ZipInputStream(InputStream, Charset)
     */
    public static ZipInputStream newZipInputStream(final InputStream is, final Charset charset) {
        return new ZipInputStream(is, checkCharset(charset));
    }

    /**
     * Creates a new ZipOutputStream with the specified OutputStream.
     *
     * @param os The OutputStream to be used for creating the ZipOutputStream.
     * @return A new ZipOutputStream instance.
     * @see ZipOutputStream#ZipOutputStream(OutputStream)
     */
    public static ZipOutputStream newZipOutputStream(final OutputStream os) {
        return new ZipOutputStream(os);
    }

    /**
     * Creates a new ZipOutputStream with the specified OutputStream and Charset.
     *
     * @param os      The OutputStream to be used for creating the ZipOutputStream.
     * @param charset The Charset to be used for creating the ZipOutputStream.
     * @return A new ZipOutputStream instance.
     * @see ZipOutputStream#ZipOutputStream(OutputStream, Charset)
     */
    public static ZipOutputStream newZipOutputStream(final OutputStream os, final Charset charset) {
        return new ZipOutputStream(os, checkCharset(charset));
    }

    /**
     * Creates a new BrotliInputStream instance for the specified input stream.
     *
     * @param is The input stream to be used for creating the BrotliInputStream.
     * @return A new BrotliInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
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
     * Closes the provided {@code URLConnection} if it's a {@code HttpURLConnection} by disconnection.
     *
     * @param conn the connection to close.
     */
    public static void close(final URLConnection conn) {
        if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection) conn).disconnect();
        }
    }

    /**
     * Closes the provided AutoCloseable object.
     * If the object is {@code null}, this method does nothing.
     *
     * @param closeable The AutoCloseable object to be closed. It can be {@code null}.
     */
    public static void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Closes the provided AutoCloseable object and handles any exceptions that occur during the closing operation.
     * The provided Consumer is used to handle any exceptions that are thrown.
     *
     * @param closeable        The AutoCloseable object to be closed, may be {@code null}.
     * @param exceptionHandler The Consumer to handle any exceptions thrown during the close operation, not {@code null}.
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
     * Closes all provided AutoCloseable objects.
     * If an AutoCloseable object is {@code null}, it will be ignored.
     *
     * @param a The AutoCloseable objects to be closed. It may contain {@code null} elements.
     */
    @SafeVarargs
    public static void closeAll(final AutoCloseable... a) {
        if (N.isEmpty(a)) {
            return;
        }

        closeAll(Arrays.asList(a));
    }

    /**
     * Closes all provided AutoCloseable objects in the Iterable.
     * If an AutoCloseable object is {@code null}, it will be ignored.
     *
     * @param c The Iterable of AutoCloseable objects to be closed. It may contain {@code null} elements.
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
            throw ExceptionUtil.toRuntimeException(ex, true);
        }
    }

    /**
     * Closes the provided AutoCloseable object quietly.
     * Any exceptions that occur during the closing operation are ignored.
     *
     * @param closeable The AutoCloseable object to be closed. It can be {@code null}.
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
     * Closes all provided AutoCloseable objects quietly.
     * Any exceptions that occur during the closing operation are ignored.
     *
     * @param a The AutoCloseable objects to be closed. It may contain {@code null} elements.
     */
    @SafeVarargs
    public static void closeAllQuietly(final AutoCloseable... a) {
        if (N.isEmpty(a)) {
            return;
        }

        closeAllQuietly(Arrays.asList(a));
    }

    /**
     * Closes all provided AutoCloseable objects quietly.
     * Any exceptions that occur during the closing operation are ignored.
     *
     * @param c The Iterable of AutoCloseable objects to be closed. It may contain {@code null} elements.
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
    //     * @throws UncheckedIOException if an I/O error occurs.
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
    //     * @throws UncheckedIOException if an I/O error occurs.
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
    //     * @throws UncheckedIOException if an I/O error occurs.
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
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public static void copyToDirectory(final File srcFile, final File destDir) throws UncheckedIOException {
        copyToDirectory(srcFile, destDir, true);
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     * If the source is a directory, all its contents will be copied into the destination directory.
     *
     * @param srcFile          The source file or directory to be copied. It must not be {@code null}.
     * @param destDir          The destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @param preserveFileDate If {@code true}, the last modified date of the file will be preserved in the copied file.
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public static void copyToDirectory(final File srcFile, final File destDir, final boolean preserveFileDate) throws UncheckedIOException {
        copyToDirectory(srcFile, destDir, preserveFileDate, Fn.BiPredicates.alwaysTrue());
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     * If the source is a directory, all its contents will be copied into the destination directory.
     *
     * @param <E>              The type of the exception that may be thrown by the filter.
     * @param srcFile          The source file or directory to be copied. It must not be {@code null}.
     * @param destDir          The destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @param preserveFileDate If {@code true}, the last modified date of the file will be preserved in the copied file.
     * @param filter           A BiPredicate that takes the source and destination files as arguments and returns a boolean. If the predicate returns {@code true}, the file is copied; if it returns {@code false}, the file is not copied.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E                    If the filter throws an exception.
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
     * @param srcDir           the validated source directory, must not be {@code null}
     * @param destDir          the validated destination directory, must not be {@code null}
     * @param preserveFileDate whether to preserve the file date
     * @param filter           the filter to apply, {@code null} means copy all directories and files
     * @throws IOException if an I/O error occurs.             if an error occurs
     * @throws E           the e
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
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
            //noinspection ResultOfMethodCallIgnored
            destDir.setLastModified(srcDir.lastModified()); //NOSONAR
        }
    }

    /**
     * Internal copy file method.
     *
     * @param srcFile          the validated source file, must not be {@code null}
     * @param destFile         the validated destination file, must not be {@code null}
     * @param preserveFileDate whether to preserve the file date
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
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
            //noinspection ResultOfMethodCallIgnored
            destFile.setLastModified(srcFile.lastModified()); //NOSONAR
        }
    }

    /**
     * Copies a file or directory from the source path to the target path.
     *
     * @param source  The source path of the file or directory to be copied.
     * @param target  The target path where the file or directory will be copied to.
     * @param options Optional arguments that specify how the copy should be done.
     * @return The path to the target file or directory.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see Files#copy(Path, Path, CopyOption...)
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
     * Copies the content of the given InputStream to the specified target Path.
     *
     * @param in      The InputStream to be copied.
     * @param target  The target Path where the InputStream content will be copied to.
     * @param options Optional arguments that specify how the copy should be done.
     * @return The number of bytes read or skipped and written to the target Path.
     * @throws UncheckedIOException if an I/O error occurs.
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
     * Copies the content of the file at the given source Path to the specified OutputStream.
     *
     * @param source The source Path of the file to be copied.
     * @param output The OutputStream where the file content will be copied to.
     * @return The number of bytes read or skipped and written to the OutputStream.
     * @throws UncheckedIOException if an I/O error occurs during the copy operation.
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
     * Copies bytes from the URL {@code source} to a file
     * {@code destination}. The directories up to {@code destination}
     * will be created if they don't already exist. {@code destination}
     * will be overwritten if it already exists.
     * <p>
     * Warning: this method does not set a connection or read timeout and thus
     * might block forever. Use {@link #copyURLToFile(URL, File, int, int)}
     * with reasonable timeouts to prevent this.
     *
     * @param source      the {@code URL} to copy bytes from, must not be {@code null}
     * @param destination the non-directory {@code File} to write bytes to
     *                    (possibly overwriting), must not be {@code null}
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
     * Copies bytes from the URL {@code source} to a file
     * {@code destination}. The directories up to {@code destination}
     * will be created if they don't already exist. {@code destination}
     * will be overwritten if it already exists.
     *
     * @param source            the {@code URL} to copy bytes from, must not be {@code null}
     * @param destination       the non-directory {@code File} to write bytes to
     *                          (possibly overwriting), must not be {@code null}
     * @param connectionTimeout the number of milliseconds until this method
     *                          will timeout if no connection could be established to the {@code source}
     * @param readTimeout       the number of milliseconds until this method will
     *                          timeout if no data could be read from the {@code source}
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
     * Moves a file from the source path to the target directory.
     *
     * @param srcFile The source file to be moved.
     * @param destDir The target directory where the file will be moved to.
     * @throws UncheckedIOException if an I/O error occurs..
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
     * Moves a file from the source path to the target path.
     *
     * @param source  The source Path of the file to be moved.
     * @param target  The target Path where the file will be moved to.
     * @param options Optional arguments that specify how the move should be done.
     * @return The target path.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see Files#move(Path, Path, CopyOption...)
     */
    public static Path move(final Path source, final Path target, final CopyOption... options) throws UncheckedIOException {
        try {
            return Files.copy(source, target, options);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Renames the specified source file to the new file name provided.
     *
     * @param srcFile     The source file to be renamed.
     * @param newFileName The new name for the file.
     * @return {@code true} if the renaming succeeded, {@code false} otherwise.
     */
    public static boolean renameTo(final File srcFile, final String newFileName) {
        return srcFile.renameTo(new File(srcFile.getParent() + IOUtil.DIR_SEPARATOR + newFileName));
    }

    /**
     * Deletes the specified file (or directory).
     *
     * @param file
     * @return {@code true} if the file is deleted successfully, otherwise {@code false} if the file is {@code null} or doesn't exist, or can't be deleted.
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
     * Deletes the specified file or directory, if it exists, without throwing an exception.
     *
     * @param file The file or directory to be deleted.
     * @return {@code true} if the file or directory was deleted successfully, {@code false} otherwise.
     * @see File#delete()
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
     * Deletes the specified file and all its sub files/directories if it's a directory.
     *
     * @param file
     * @return {@code true} if the file is deleted successfully, otherwise {@code false} if the file is {@code null} or doesn't exist, or can't be deleted.
     * @see File#delete()
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
     * Deletes the specified file if it's a file or its sub files/directories if it's a directory.
     *
     * @param file The file or directory to be deleted.
     * @return {@code true} if the file or directory was deleted successfully, {@code false} otherwise.
     * @see File#delete()
     * @see Files#delete(Path)
     */
    public static boolean deleteFiles(final File file) {
        return deleteFiles(file, BiPredicates.alwaysTrue());
    }

    /**
     * Deletes the specified file if it's a file or its sub files/directories if it's a directory with the specified filter.
     *
     * @param <E>    the type of the exception that may be thrown
     * @param file   The file or directory to be deleted.
     * @param filter The filter to be applied when deleting files or directories.
     * @return {@code true} if the file or directory was deleted successfully, {@code false} otherwise.
     * @throws E if an exception of type E occurs during the operation.
     * @see File#delete()
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
     * Deletes the sub files/directories under the specified {@code directory}. The {@code directory} itself won't be deleted.
     *
     * @param directory directory to clean
     * @return {@code false} if some of its sub files can't be deleted.
     * @see #deleteFiles(File, Throwables.BiPredicate)
     */
    public static boolean cleanDirectory(final File directory) {
        if (directory == null || !directory.isDirectory()) {
            return true;
        }

        return deleteFiles(directory, BiPredicates.alwaysTrue());
    }

    static void createNewFileIfNotExists(final File file) throws IOException {
        if (!file.exists() && (!file.createNewFile())) { //NOSONAR
            throw new IOException("Failed to create new file: " + file.getName());
        }
    }

    /**
     * Creates a new file if it does not exist
     *
     * @param file The file to be checked and possibly created.
     * @return {@code false} if the file already exists or if the file could not be created, {@code true} otherwise.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean createIfNotExists(final File file) throws UncheckedIOException {
        try {
            return !file.exists() && file.createNewFile();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new directory if it does not exist.
     *
     * @param dir The directory to be checked and possibly created.
     * @return {@code true} if the directory was created successfully or already exists, {@code false} otherwise.
     */
    public static boolean mkdirIfNotExists(final File dir) {
        if (!(dir.exists() && dir.isDirectory())) {
            return dir.mkdir();
        }

        return false;
    }

    /**
     * Creates new directories if they do not exist.
     *
     * @param dir The directory to be checked and possibly created.
     * @return {@code true} if the directories were created successfully or already exist, {@code false} otherwise.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean mkdirsIfNotExists(final File dir) {
        if (!(dir.exists() && dir.isDirectory())) {
            return dir.mkdirs();
        }

        return false;
    }

    /**
     * Checks if the provided Reader is an instance of {@code java.io.BufferedReader}.
     *
     * @param reader The Reader to be checked.
     * @return {@code true} if the Reader is an instance of BufferedReader, {@code false} otherwise.
     */
    public static boolean isBufferedReader(final Reader reader) {
        return reader instanceof java.io.BufferedReader;
    }

    /**
     * Checks if the provided Writer is an instance of {@code java.io.BufferedWriter}.
     *
     * @param writer The Writer to be checked.
     * @return {@code true} if the Writer is an instance of BufferedWriter, {@code false} otherwise.
     */
    public static boolean isBufferedWriter(final Writer writer) {
        return writer instanceof java.io.BufferedWriter;
    }

    /**
     * Checks if the specified {@code File} is newer than the specified {@code Date}.
     *
     * @param file the {@code File} of which the modification date must be compared
     * @param date the date reference
     * @return {@code true} if the {@code File} exists and has been modified after the given {@code Date}.
     * @throws IllegalArgumentException if the file or date is {@code null}
     */
    public static boolean isFileNewer(final File file, final Date date) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(date, cs.date);

        return file.lastModified() > date.getTime();
    }

    /**
     * Checks if the specified {@code File} is newer than the reference {@code File}.
     *
     * @param file      the {@code File} of which the modification date must be compared
     * @param reference the {@code File} of which the modification date is used
     * @return {@code true} if the {@code File} exists and has been modified more recently than the reference {@code File}
     * @throws IllegalArgumentException if the file or reference file is {@code null} or the reference file doesn't exist
     */
    public static boolean isFileNewer(final File file, final File reference) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(reference, cs.reference);

        return file.lastModified() > reference.lastModified();
    }

    /**
     * Checks if the specified {@code File} is older than the specified {@code Date}.
     *
     * @param file the {@code File} of which the modification date must be compared
     * @param date the date reference
     * @return {@code true} if the {@code File} exists and has been modified before the given {@code Date}
     * @throws IllegalArgumentException if the file or date is {@code null}
     */
    public static boolean isFileOlder(final File file, final Date date) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(date, cs.date);

        return file.lastModified() < date.getTime();
    }

    /**
     * Checks if the specified {@code File} is older than the reference {@code File}.
     *
     * @param file      the {@code File} of which the modification date must be compared
     * @param reference the {@code File} of which the modification date is used
     * @return {@code true} if the {@code File} exists and has been modified before the reference {@code File}
     * @throws IllegalArgumentException if the file or reference file is {@code null} or the reference file doesn't exist
     */
    public static boolean isFileOlder(final File file, final File reference) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(reference, cs.reference);

        return file.lastModified() < reference.lastModified();
    }

    /**
     * Checks if the specified file is a Symbolic Link rather than an actual file.
     *
     * @param file The file to be checked.
     * @return {@code true} if the file is a Symbolic Link, {@code false} otherwise.
     * @throws IllegalArgumentException if the file is {@code null}.
     * @see Files#isSymbolicLink(Path)
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
     * @return the length of the file, or recursive size of the directory, provided (in bytes).
     * @throws IllegalArgumentException if the file is {@code null} or the file does not exist.
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

    private static long sizeOf0(final File file) {
        if (file.isDirectory()) {
            return sizeOfDirectory0(file);
        }

        return file.length(); // will be 0 if file does not exist
    }

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
     * Compresses the specified source file and writes the compressed data to the target file using the ZIP compression algorithm.
     *
     * @param sourceFile The file to be compressed. This must be a valid file.
     * @param targetFile The file to which the compressed data will be written. This must be a valid file.
     * @throws UncheckedIOException if an I/O error occurs.
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
     * Compresses the specified source files and writes the compressed data to the target file using the ZIP compression algorithm.
     *
     * @param sourceFiles The collection of files to be compressed. Each must be a valid file.
     * @param targetFile  The file to which the compressed data will be written. This must be a valid file.
     * @throws UncheckedIOException if an I/O error occurs.
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
     * Compresses the specified source file and writes the compressed data to the target file using the provided ZipOutputStream.
     * This is a helper method used in the process of creating a ZIP file.
     *
     * @param sourceFile The file to be compressed. This must be a valid file.
     * @param zos        The ZipOutputStream to which the compressed data will be written.
     * @param targetFile The file to which the compressed data will be written. This must be a valid file.
     * @throws IOException if an I/O error occurs.
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
     * Compresses the specified file and writes the compressed data to the target file using the provided ZipOutputStream.
     * This is a helper method used in the process of creating a ZIP file.
     *
     * @param file       The file to be compressed. This must be a valid file.
     * @param sourceDir  The directory containing the file to be compressed.
     * @param zos        The ZipOutputStream to which the compressed data will be written.
     * @param targetFile The file to which the compressed data will be written. This must be a valid file.
     * @throws IOException if an I/O error occurs during the process.
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
     * Unzips the specified source ZIP file to the target directory.
     *
     * @param srcZipFile The source ZIP file to be unzipped. This must be a valid ZIP file.
     * @param targetDir  The directory to which the contents of the ZIP file will be extracted. This must be a valid directory.
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
     * Splits the specified file into a number of parts.
     *
     * @param file         The file to be split. This must be a valid file.
     * @param countOfParts The number of parts the file should be split into.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    public static void split(final File file, final int countOfParts) throws UncheckedIOException {
        split(file, countOfParts, file.getParentFile());
    }

    /**
     * Splits the specified file into a number of parts and stores them in the specified directory.
     *
     * @param file         The file to be split. This must be a valid file.
     * @param countOfParts The number of parts the file should be split into.
     * @param destDir      The directory where the split parts should be stored.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    public static void split(final File file, final int countOfParts, final File destDir) throws UncheckedIOException {
        final long sizeOfPart = (file.length() % countOfParts) == 0 ? (file.length() / countOfParts) : (file.length() / countOfParts) + 1;

        splitBySize(file, sizeOfPart, destDir);
    }

    /**
     * Splits the specified file into parts of a given size.
     *
     * @param file       The file to be split. This must be a valid file.
     * @param sizeOfPart The size of each part after the split.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    public static void splitBySize(final File file, final long sizeOfPart) throws UncheckedIOException {
        splitBySize(file, sizeOfPart, file.getParentFile());
    }

    /**
     * Splits the specified file into parts of a given size and stores them in the specified directory.
     *
     * @param file       The file to be split. This must be a valid file.
     * @param sizeOfPart The size of each part after the split.
     * @param destDir    The directory where the split parts will be stored.
     * @throws UncheckedIOException if an I/O error occurs during the process.
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
                final String subFileName = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + fileName + "_"
                        + Strings.padStart(N.stringOf(fileSerNum++), 4, '0');
                output = IOUtil.newFileOutputStream(new File(subFileName));
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
     * Splits the specified file into a number of parts by line.
     *
     * @param file       The file to be split. This must be a valid file.
     * @param numOfParts The number of parts the file should be split into.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    static void splitByLine(final File file, final int numOfParts) throws UncheckedIOException {
        splitByLine(file, numOfParts, file.getParentFile());
    }

    /**
     * Splits the specified file into a number of parts by line.
     *
     * @param file       The file to be split. This must be a valid file.
     * @param numOfParts The number of parts the file should be split into.
     * @param destDir    The directory where the split parts will be stored.
     * @throws UncheckedIOException if an I/O error occurs during the process.
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

            String subFileName = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + prefix + "_" + Strings.padStart(N.stringOf(fileSerNum++), suffixLen, '0')
                    + postfix;
            bw = Objectory.createBufferedWriter(IOUtil.newFileWriter(new File(subFileName)));

            int lineCounter = 0;

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                bw.write(line);
                bw.write(IOUtil.LINE_SEPARATOR);
                lineCounter++;

                if ((lineCounter % lineNumOfPart) == 0) {
                    close(bw);
                    Objectory.recycle(bw);
                    bw = null;

                    subFileName = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + prefix + "_" + Strings.padStart(N.stringOf(fileSerNum++), suffixLen, '0')
                            + postfix;
                    bw = Objectory.createBufferedWriter(IOUtil.newFileWriter(new File(subFileName)));
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
     * @throws UncheckedIOException if an I/O error occurs.
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
     * Merges the given source files into the specified destination file.
     *
     * @param sourceFiles An array of files to be merged. These must be valid files.
     * @param destFile    The destination file where the merged content will be written. This must be a valid file.
     * @return The number of bytes written to the destination file.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    public static long merge(final File[] sourceFiles, final File destFile) throws UncheckedIOException {
        return merge(Array.asList(sourceFiles), destFile);
    }

    /**
     * Merges the given source files into the specified destination file.
     *
     * @param sourceFiles A collection of files to be merged. These must be valid files.
     * @param destFile    The destination file where the merged content will be written. This must be a valid file.
     * @return The number of bytes written to the destination file.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    public static long merge(final Collection<File> sourceFiles, final File destFile) throws UncheckedIOException {
        return merge(sourceFiles, N.EMPTY_BYTE_ARRAY, destFile);
    }

    /**
     * Merges the given source files into the specified destination file, separated by the provided delimiter.
     *
     * @param sourceFiles A collection of files to be merged. These must be valid files.
     * @param delimiter   A byte array that will be inserted between each file during the merge.
     * @param destFile    The destination file where the merged content will be written. This must be a valid file.
     * @return The number of bytes written to the destination file.
     * @throws UncheckedIOException if an I/O error occurs during the process.
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
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static String getAbsolutePath(final File parentDir, String relativeFilePath) {
        String newRelativePath = "";

        for (int i = 0; i < relativeFilePath.length(); i++) {
            final char c = relativeFilePath.charAt(i);

            if ((c == '\\') || (c == '/')) {
                //noinspection StringConcatenationInLoop
                newRelativePath += File.separator; //NOSONAR
            } else {
                //noinspection StringConcatenationInLoop
                newRelativePath += c; //NOSONAR
            }
        }

        relativeFilePath = newRelativePath;

        final String path = parentDir.getAbsolutePath() + File.separator + relativeFilePath;

        final File dir = new File(path.substring(0, path.lastIndexOf(File.separator)));

        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
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
     * Lists the names of all files and directories in the specified parent directory.
     *
     * @param parentPath The parent directory from which to list files and directories.
     * @return A list of strings representing the names of all files and directories in the parent directory.
     */
    public static List<String> list(final File parentPath) {
        return list(parentPath, false, false);
    }

    /**
     * Lists the names of all files and directories in the specified parent directory.
     * If the recursive parameter is set to {@code true}, it will list files in all subdirectories as well.
     * If the excludeDirectory parameter is set to {@code true}, it will exclude directories from the list.
     *
     * @param parentPath       The parent directory from which to list files and directories.
     * @param recursively      A boolean indicating whether to list files in all subdirectories.
     * @param excludeDirectory A boolean indicating whether to exclude directories from the list.
     * @return A list of strings representing the names of all files and directories in the parent directory.
     */
    public static List<String> list(final File parentPath, final boolean recursively, final boolean excludeDirectory) {
        return list(parentPath, recursively, excludeDirectory ? directories_excluded_filter : all_files_filter);
    }

    /**
     * Lists the names of files in the specified parent directory.
     * If the recursive parameter is set to {@code true}, it will list files in all subdirectories as well.
     *
     * @param <E>         The type of the exception that may be thrown by the filter.
     * @param parentPath  The parent directory where the listing will start. It must not be {@code null}.
     * @param recursively If {@code true}, files in all subdirectories of the parent directory will be listed.
     * @param filter      A BiPredicate that takes the parent directory and a file as arguments and returns a boolean. If the predicate returns {@code true}, the file is listed; if it returns {@code false}, the file is not listed.
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
                //noinspection ConstantValue
                files.addAll(list(file, recursively, filter));
            }
        }

        return files;
    }

    /**
     * Lists all files in the specified parent directory.
     *
     * @param parentPath The parent directory from which to list files.
     * @return A list of File objects representing all files in the parent directory.
     */
    public static List<File> listFiles(final File parentPath) {
        return listFiles(parentPath, false, false);
    }

    /**
     * Lists all files in the specified parent directory.
     * If the recursive parameter is set to {@code true}, it will list files in all subdirectories as well.
     * If the excludeDirectory parameter is set to {@code true}, it will exclude directories from the list.
     *
     * @param parentPath       The parent directory from which to list files.
     * @param recursively      A boolean indicating whether to list files in all subdirectories.
     * @param excludeDirectory A boolean indicating whether to exclude directories from the list.
     * @return A list of File objects representing all files in the parent directory.
     */
    public static List<File> listFiles(final File parentPath, final boolean recursively, final boolean excludeDirectory) {
        return listFiles(parentPath, recursively, excludeDirectory ? directories_excluded_filter : all_files_filter);
    }

    /**
     * Lists all files in the specified parent directory.
     * If the recursive parameter is set to {@code true}, it will list files in all subdirectories as well.
     *
     * @param <E>         The type of the exception that may be thrown by the filter.
     * @param parentPath  The parent directory where the listing will start. It must not be {@code null}.
     * @param recursively If {@code true}, files in all subdirectories of the parent directory will be listed.
     * @param filter      A BiPredicate that takes the parent directory and a file as arguments and returns a boolean. If the predicate returns {@code true}, the file is listed; if it returns {@code false}, the file is not listed.
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
                //noinspection ConstantValue
                files.addAll(listFiles(file, recursively, filter));
            }
        }

        return files;
    }

    /**
     * Lists all directories in the specified parent directory.
     *
     * @param parentPath The parent directory from which to list directories.
     * @return A list of File objects representing all directories in the parent directory.
     */
    public static List<File> listDirectories(final File parentPath) {
        return listDirectories(parentPath, false);
    }

    /**
     * Lists all directories in the specified parent directory.
     * If the recursive parameter is set to {@code true}, it will list directories in all subdirectories as well.
     *
     * @param parentPath  The parent directory from which to list directories.
     * @param recursively A boolean indicating whether to list directories in all subdirectories.
     * @return A list of File objects representing all directories in the parent directory and its subdirectories if recursively is {@code true}.
     */
    public static List<File> listDirectories(final File parentPath, final boolean recursively) {
        return listFiles(parentPath, recursively, directories_only_filter);
    }

    //-----------------------------------------------------------------------

    /**
     * Convert from a {@code URL} to a {@code File}.
     * <p>
     * From version 1.1 this method will decode the URL.
     * Syntax such as {@code file:///my%20docs/file.txt} will be
     * correctly decoded to <code>/my docs/file.txt</code>. Starting with version
     * 1.5, this method uses UTF-8 to decode percent-encoded octets to characters.
     * Additionally, malformed percent-encoded octets are handled leniently by
     * passing them through literally.
     *
     * @param url the file URL to convert, {@code null} returns {@code null}
     * @return
     * @throws IllegalArgumentException if the parameter is {@code null} or the URL is not a file URL
     */
    public static File toFile(final URL url) {
        N.checkArgNotNull(url, cs.url);

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
     * {@code null}.
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
                            buffer.append(DEFAULT_CHARSET.decode(bytes));
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
     * Converts each of an array of {@code URL} to a {@code File}.
     * <p>
     * Returns an array of the same size as the input.
     * If the input is {@code null}, an empty array is returned.
     * If the input contains {@code null}, the output array contains {@code null} at the same
     * index.
     * <p>
     * This method will decode the URL.
     * Syntax such as {@code file:///my%20docs/file.txt} will be
     * correctly decoded to <code>/my docs/file.txt</code>.
     *
     * @param urls the file URLs to convert, {@code null} returns empty array
     * @return a non-{@code null} array of Files matching the input, with a {@code null} item
     * if there was a {@code null} at that index in the input array
     * @throws UncheckedIOException     if an I/O error occurs.
     * @throws IllegalArgumentException if any file is incorrectly encoded
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
     * Converts a collection of URLs into a list of corresponding File objects.
     *
     * @param urls The collection of URLs to be converted.
     * @return A list of File objects corresponding to the input URLs.
     * @throws UncheckedIOException if an I/O error occurs during the conversion.
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
     * Converts a File object into a URL.
     *
     * @param file The File object to be converted.
     * @return A URL object corresponding to the input File object.
     * @throws UncheckedIOException if an I/O error occurs during the conversion.
     * @see File#toURI()
     * @see URI#toURL()
     */
    public static URL toURL(final File file) throws UncheckedIOException {
        try {
            return file.toURI().toURL();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Converts an array of File objects into an array of corresponding URL objects.
     *
     * @param files The array of File objects to be converted.
     * @return An array of URL objects corresponding to the input File objects.
     * @throws UncheckedIOException if an I/O error occurs during the conversion.
     * @see File#toURI()
     * @see URI#toURL()
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
     * Converts a collection of File objects into a list of corresponding URL objects.
     *
     * @param files The collection of File objects to be converted.
     * @return A list of URL objects corresponding to the input File objects.
     * @throws UncheckedIOException if an I/O error occurs during the conversion.
     * @see File#toURI()
     * @see URI#toURL()
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
     * @return {@code true} if the file exists and last modified time is updated successfully.
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
     * @return {@code true} if the content of the streams are equal or they both don't
     * exist, {@code false} otherwise
     * @throws IOException if an I/O error occurs.          if an I/O error occurs
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
     * @return {@code true} if the content of the readers are equal or they both don't exist, {@code false} otherwise
     * @throws IOException if an I/O error occurs. if an I/O error occurs
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
     * @return {@code true} if the content of the readers are equal (ignoring EOL differences),  {@code false} otherwise
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static boolean contentEqualsIgnoreEOL(final Reader input1, final Reader input2) throws IOException {
        if (input1 == input2) {
            return true;
        }

        if (input1 == null || input2 == null) {
            return false;
        }

        final boolean isInput1BufferedReader = IOUtil.isBufferedReader(input1);
        final boolean isInput2BufferedReader = IOUtil.isBufferedReader(input2);
        final BufferedReader br1 = isInput1BufferedReader ? (BufferedReader) input1 : Objectory.createBufferedReader(input1); //NOSONAR
        final BufferedReader br2 = isInput2BufferedReader ? (BufferedReader) input2 : Objectory.createBufferedReader(input2); //NOSONAR

        try {
            String line1 = br1.readLine();
            String line2 = br2.readLine();

            while (line1 != null && line1.equals(line2)) {
                line1 = br1.readLine();
                line2 = br2.readLine();
            }

            return line1 == null && line2 == null;
        } finally {
            if (!isInput1BufferedReader) {
                Objectory.recycle(br1);
            }

            if (!isInput2BufferedReader) {
                Objectory.recycle(br2);
            }
        }
    }

    /**
     * Execute the specified {@code cmd} command on each lines from the specified {@code source} File.
     *
     * @param <E>        The type of exception that the lineParser can throw.
     * @param source     The source file to be parsed.
     * @param cmd A Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E                    If the lineParser throws an exception.
     * @see #parse(File, Throwables.Consumer)
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    @Beta
    public static <E extends Exception> void forLines(final File source, final Throwables.Consumer<? super String, E> cmd) throws UncheckedIOException, E {
        parse(source, cmd, Fn.emptyAction());
    }

    /**
     * Execute the specified {@code cmd} command on each lines from the specified {@code source} Reader.
     *
     * @param <E> the type of exception that the lineParser can throw
     * @param source the reader to be parsed
     * @param cmd a consumer that takes a line of the reader as a string and performs the desired operation
     * @throws UncheckedIOException if an I/O error occurs
     * @throws E if the lineParser throws an exception
     * @see #parse(Reader, Throwables.Consumer)
     */
    @Beta
    public static <E extends Exception> void forLines(final Reader source, final Throwables.Consumer<? super String, E> cmd) throws UncheckedIOException, E {
        parse(source, cmd, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>        The type of exception that the lineParser can throw.
     * @param source     The source file to be parsed.
     * @param lineParser A Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E                    If the lineParser throws an exception.
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final File source, final long lineOffset, final long count, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>              The type of exception that the lineParser can throw.
     * @param <E2>             The type of exception that the onComplete can throw.
     * @param source           The source file/directory to be parsed.
     * @param lineOffset       The line number from where to start parsing.
     * @param count            The number of lines to be parsed.
     * @param processThreadNum The number of threads to be used for parsing.
     * @param queueSize        The size of the queue for holding lines to be parsed.
     * @param lineParser       A Consumer that takes a line of the file as a String and performs the desired operation.
     * @param onComplete       A Runnable that is executed after the parsing is complete.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E                    If the lineParser throws an exception.
     * @throws E2                   If the onComplete throws an exception.
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @param <E>        The type of exception that the lineParser can throw.
     * @param files      The collection of files to be parsed.
     * @param lineParser A Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E                    If the lineParser throws an exception.
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line using the provided lineParser.
     *
     * @param <E>        The type of exception that the lineParser can throw.
     * @param <E2>       The type of exception that the onComplete can throw.
     * @param files      The collection of files to be parsed.
     * @param lineOffset The line number from where to start parsing.
     * @param count      The number of lines to be parsed.
     * @param lineParser A Consumer that takes a line of the file as a String and performs the desired operation.
     * @param onComplete A Runnable that is executed after the parsing is complete.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws E                    If the lineParser throws an exception.
     * @throws E2                   If the onComplete throws an exception.
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @param source           parse all the sub files recursively if the element is a directory.
     * @param lineOffset
     * @param count
     * @param readThreadNum    new threads started to parse/process the lines/records
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize        size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @param files            parse all the sub files recursively if the element is a directory.
     * @param lineOffset
     * @param count
     * @param readThreadNum    new threads started to parse/process the lines/records
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize        size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void parse(final InputStream source, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineOffset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize        size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @see #parse(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
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
     * @param queueSize        size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    the e
     * @throws E2                   the e2
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Reader source, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        Iterators.forEach(new LineIterator(source), lineOffset, count, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
     * @param source
     * @param outputZipFile
     * @return
     * @throws IOException if an I/O error occurs.
     */
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
     * @param cs
     * @param charset
     * @return
     */
    private static byte[] toByteArray(final CharSequence cs, final Charset charset) {
        return String.valueOf(cs).getBytes(charset);
    }
}
