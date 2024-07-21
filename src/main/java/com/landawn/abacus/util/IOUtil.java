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
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
            Class<?> cls = ClassUtil.forClass("java.lang.StringCoding");
            Method enMethod = ClassUtil.getDeclaredMethod(cls, "encode", Charset.class, char[].class, int.class, int.class);
            Method deMethod = ClassUtil.getDeclaredMethod(cls, "decode", Charset.class, byte[].class, int.class, int.class);

            if (enMethod != null && deMethod != null) {
                ClassUtil.setAccessible(enMethod, true);
                ClassUtil.setAccessible(deMethod, true);

                char[] chars = "abc".toCharArray();
                byte[] bytes = ClassUtil.invokeMethod(enMethod, DEFAULT_CHARSET, chars, 1, 1);
                char[] chars2 = ClassUtil.invokeMethod(deMethod, DEFAULT_CHARSET, bytes, 0, bytes.length);

                if (chars2.length == 1 && chars2[0] == 'b') {
                    encodeMethod = enMethod;
                    decodeMethod = deMethod;
                }
            }

        } catch (Exception e) {
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

    /** current path retrieved by {@code new File("./").getAbsolutePath()} */
    public static final String CURRENT_PATH;

    static {
        final String path = new File("./").getAbsolutePath();
        CURRENT_PATH = path.charAt(path.length() - 1) == '.' ? path.substring(0, path.length() - 1) : path;
    }

    // ...
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

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
                } catch (Exception e) {
                    logger.error("Failed to get host name");
                }
            } else {
                try {
                    ret = InetAddress.getLocalHost().getHostName();
                } catch (Exception e) {
                    logger.error("Failed to get host name");
                }
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Charset checkCharset(Charset charset) {
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
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static byte[] readAllBytes(final File source) throws UncheckedIOException {
        try {
            return readBytes(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static byte[] readAllBytes(final InputStream source) throws UncheckedIOException {
        try {
            return readBytes(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param source
     * @return
     * @throws IOException the unchecked IO exception
     * @see #readAllBytes(File)
     */
    public static byte[] readBytes(final File source) throws IOException {
        return readBytes(source, 0, Integer.MAX_VALUE);
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
     * @throws IOException
     * @see #readAllBytes(InputStream)
     */
    public static byte[] readBytes(final InputStream source) throws IOException {
        return readBytes(source, 0, Integer.MAX_VALUE);
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
        if ((maxLen == 0) || ((offset > 0) && (skip(source, offset) < offset))) {
            return N.EMPTY_BYTE_ARRAY;
        }

        ByteArrayOutputStream os = null;
        final byte[] buf = Objectory.createByteArrayBuffer();
        final int bufLength = buf.length;

        int totalCount = 0;
        int count = 0;

        try {
            while (totalCount < maxLen && EOF != (count = read(source, buf, 0, Math.min(maxLen - totalCount, bufLength)))) {
                if ((count == bufLength && count < maxLen) && (os == null)) {
                    os = Objectory.createByteArrayOutputStream();
                }

                if (os != null) {
                    os.write(buf, 0, count);
                }

                totalCount += count;
            }

            return os == null ? (totalCount <= 0 ? N.EMPTY_BYTE_ARRAY : N.copyOfRange(buf, 0, totalCount)) : os.toByteArray();

        } finally {
            Objectory.recycle(buf);
            Objectory.recycle(os);
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final File source) throws UncheckedIOException {
        try {
            return readChars(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final File source, final Charset encoding) throws UncheckedIOException {
        try {
            return readChars(source, encoding, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final InputStream source) throws UncheckedIOException {
        try {
            return readChars(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final InputStream source, final Charset encoding) throws UncheckedIOException {
        try {
            return readChars(source, encoding, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final Reader source) throws UncheckedIOException {
        try {
            return readChars(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param source
     * @return
     * @throws IOException
     * @see #readAllChars(File)
     */
    public static char[] readChars(final File source) throws IOException {
        return readChars(source, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param source
     * @param encoding
     * @return
     * @throws IOException
     * @see #readAllChars(File, Charset)
     */
    public static char[] readChars(final File source, final Charset encoding) throws IOException {
        return readChars(source, encoding, 0, Integer.MAX_VALUE);
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
     * @throws IOException
     * @see #readAllChars(InputStream)
     */
    public static char[] readChars(final InputStream source) throws IOException {
        return readChars(source, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param source
     * @param encoding
     * @return
     * @throws IOException
     * @see #readAllChars(InputStream, encoding)
     */
    public static char[] readChars(final InputStream source, final Charset encoding) throws IOException {
        return readChars(source, encoding, 0, Integer.MAX_VALUE);
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

        Reader reader = null;

        // try {
        reader = createReader(source, encoding);
        // } finally {
        // // close(reader);
        // }

        return readChars(reader, offset, maxLen);
    }

    /**
     *
     * @param source
     * @return
     * @throws IOException
     * @see #readAllChars(Reader)
     */
    public static char[] readChars(final Reader source) throws IOException {
        return readChars(source, 0, Integer.MAX_VALUE);
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
        if ((maxLen == 0) || ((offset > 0) && (skip(source, offset) < offset))) {
            return N.EMPTY_CHAR_ARRAY;
        }

        StringBuilder sb = null;
        final char[] buf = Objectory.createCharArrayBuffer();
        final int bufLength = buf.length;

        int totalCount = 0;
        int count = 0;

        try {
            while (totalCount < maxLen && EOF != (count = read(source, buf, 0, Math.min(maxLen - totalCount, bufLength)))) {
                if ((count == bufLength && count < maxLen) && (sb == null)) {
                    sb = Objectory.createBigStringBuilder();
                }

                if (sb != null) {
                    sb.append(buf, 0, count);
                }

                totalCount += count;
            }

            if (sb == null) {
                return totalCount <= 0 ? N.EMPTY_CHAR_ARRAY : N.copyOfRange(buf, 0, totalCount);
            } else {
                final char[] a = new char[totalCount];
                sb.getChars(0, totalCount, a, 0);

                return a;
            }
        } finally {
            Objectory.recycle(buf);
            Objectory.recycle(sb);
        }
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final File source) throws UncheckedIOException {
        try {
            return readToString(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final File source, final Charset encoding) throws UncheckedIOException {
        try {
            return readToString(source, encoding, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final InputStream source) throws UncheckedIOException {
        try {
            return readToString(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final InputStream source, final Charset encoding) throws UncheckedIOException {
        try {
            return readToString(source, encoding, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Read all content into one {@code String}.
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readAllToString(final Reader source) throws UncheckedIOException {
        try {
            return readToString(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
    @SuppressWarnings("deprecation")
    public static String readToString(final File source, final Charset encoding, final long offset, final int maxLen) throws IOException {
        final char[] chs = readChars(source, encoding, offset, maxLen);

        return N.isEmpty(chs) ? Strings.EMPTY_STRING : InternalUtil.newString(chs, true);
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
     *
     * @param source
     * @param encoding
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public static String readToString(final InputStream source, final Charset encoding, final long offset, final int maxLen) throws IOException {
        final char[] chs = readChars(source, encoding, offset, maxLen);

        return N.isEmpty(chs) ? Strings.EMPTY_STRING : InternalUtil.newString(chs, true);
    }

    /**
     *
     * @param source
     * @param offset
     * @param maxLen
     * @return
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    public static String readToString(final Reader source, final long offset, final int maxLen) throws IOException {
        final char[] chs = readChars(source, offset, maxLen);

        return N.isEmpty(chs) ? Strings.EMPTY_STRING : InternalUtil.newString(chs, true);
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final File source) throws UncheckedIOException {
        try {
            return readLines(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final File source, final Charset encoding) throws UncheckedIOException {
        try {
            return readLines(source, encoding, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final InputStream source) throws UncheckedIOException {
        try {
            return readLines(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final InputStream source, final Charset encoding) throws UncheckedIOException {
        try {
            return readLines(source, encoding, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * <br />
     * Note: It should not be used to read {@code File/InputStream/Reader} with size closed to {@code Integer.MAX_VALUE}.
     *
     * @param source
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final Reader source) throws UncheckedIOException {
        try {
            return readLines(source, 0, Integer.MAX_VALUE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
            String result = null;

            if (lineIndex == 0) {
                result = br.readLine(); //NOSONAR
            } else {
                while (lineIndex-- > 0 && br.readLine() != null) {
                    // continue
                }

                result = br.readLine(); //NOSONAR
            }

            // Has trouble for reading first/last line from empty file? // TODO
            //    if (result == null) {
            //        throw new IndexOutOfBoundsException("lineIndex: " + lineIndex + " excceded the total line count of the specified reader/file"); // Should throw IllegalArgumentException
            //    }

            return result;
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
     *
     * @param source
     * @param buf
     * @return
     * @throws IOException
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
            int n1 = source.read(buf, off + n, len - n);

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
            int n1 = source.read(buf, off + n, len - n);

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

        boolean isBufferedWriter = isBufferedWriter(output);
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

        boolean isBufferedWriter = isBufferedWriter(output);
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR

        try {
            for (Object line : lines) {
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
     * @param str
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence str, final File output) throws IOException {
        write(str, DEFAULT_CHARSET, output);
    }

    /**
     *
     * @param str
     * @param charset
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence str, Charset charset, final File output) throws IOException {
        charset = checkCharset(charset);

        write(chars2Bytes(toCharArray(str), charset), output);
    }

    /**
     *
     * @param str
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence str, final OutputStream output) throws IOException {
        write(str, output, false);
    }

    /**
     *
     * @param str
     * @param charset
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence str, final Charset charset, final OutputStream output) throws IOException {
        write(str, charset, output, false);
    }

    /**
     *
     * @param str
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final CharSequence str, final OutputStream output, final boolean flush) throws IOException {
        write(str, DEFAULT_CHARSET, output, flush);
    }

    /**
     *
     * @param str
     * @param charset
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final CharSequence str, Charset charset, final OutputStream output, final boolean flush) throws IOException {
        charset = checkCharset(charset);

        output.write(chars2Bytes(toCharArray(str), charset));

        if (flush) {
            output.flush();
        }
    }

    /**
     *
     * @param str
     * @param output
     * @throws IOException
     */
    public static void write(final CharSequence str, final Writer output) throws IOException {
        write(str, output, false);
    }

    /**
     *
     * @param str
     * @param output
     * @param flush
     * @throws IOException
     */
    public static void write(final CharSequence str, final Writer output, final boolean flush) throws IOException {
        write(toCharArray(str), output, flush);
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
     *
     * @param source
     * @param output
     * @return
     * @throws IOException
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
     *
     *
     * @param source
     * @param offset
     * @param count
     * @param output
     * @return
     * @throws IOException
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
     *
     *
     * @param source
     * @param offset by byte
     * @param count
     * @param output
     * @param flush
     * @return
     * @throws IOException
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
     *
     * @param source
     * @param output
     * @return
     * @throws IOException
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

            long result = write(source, offset, count, os);

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
     *
     *
     * @param source
     * @param offset by byte
     * @param count by byte
     * @param output
     * @param flush
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, "source");
        N.checkArgNotNull(output, "output");

        N.checkArgNotNegative(offset, "offset"); // NOSONAR
        N.checkArgNotNegative(count, "count"); // NOSONAR

        final byte[] buf = Objectory.createByteArrayBuffer();

        try {
            if (offset > 0) {
                long skipped = skip(source, offset);

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

            long result = write(source, offset, count, writer);

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
     *
     *
     * @param source
     * @param offset by char
     * @param count by char
     * @param output
     * @param flush
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static long write(final Reader source, final long offset, final long count, final Writer output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, "source");
        N.checkArgNotNull(output, "output");

        N.checkArgNotNegative(offset, "offset");
        N.checkArgNotNegative(count, "count");

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
     *
     * @param bytes
     * @param targetFile
     * @throws IOException
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
     * @param str
     * @param targetFile
     * @throws IOException
     */
    public static void append(CharSequence str, File targetFile) throws IOException {
        append(str, DEFAULT_CHARSET, targetFile);
    }

    /**
     *
     * @param str
     * @param charset
     * @param targetFile
     * @throws IOException
     */
    public static void append(CharSequence str, Charset charset, File targetFile) throws IOException {
        final char[] chs = toCharArray(str);

        append(chs, 0, chs.length, charset, targetFile);
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

            long result = write(source, offset, count, writer);

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
    public static void appendLine(Object obj, File targetFile) throws IOException {
        appendLine(obj, DEFAULT_CHARSET, targetFile);
    }

    /**
     *
     * @param obj
     * @param charset
     * @param targetFile
     * @throws IOException
     */
    public static void appendLine(Object obj, Charset charset, File targetFile) throws IOException {
        final char[] chs = toCharArray(obj + IOUtil.LINE_SEPARATOR);

        append(chs, 0, chs.length, charset, targetFile);
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
     * Return the count of skipped bytes.
     *
     * @param input
     * @param toSkip
     * @return
     * @throws IOException
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
                long n = read(input, buf, 0, (int) Math.min(remain, buf.length));

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
     * Return the count of skipped chars.
     *
     * @param input
     * @param toSkip
     * @return
     * @throws IOException
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
                long n = read(input, buf, 0, (int) Math.min(remain, buf.length));

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
     *
     * @param input
     * @param toSkip
     * @throws IOException if the remaining length of the specified <code>input</code> is less than the specified <code>toSkip</code>
     */
    public static void skipFully(final InputStream input, final long toSkip) throws IOException {
        if (toSkip < 0) {
            throw new IllegalArgumentException("Bytes to skip must not be negative: " + toSkip);
        }

        long skipped = skip(input, toSkip);

        if (skipped != toSkip) {
            throw new IOException("Bytes to skip: " + toSkip + " actual: " + skipped);
        }
    }

    /**
     *
     * @param input
     * @param toSkip
     * @throws IOException if the remaining length of the specified <code>input</code> is less than the specified <code>toSkip</code>
     */
    public static void skipFully(final Reader input, final long toSkip) throws IOException {
        long skipped = skip(input, toSkip);

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
    public static MappedByteBuffer map(File file) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file);

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
    public static MappedByteBuffer map(File file, MapMode mode) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file);
        N.checkArgNotNull(mode);

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
    public static MappedByteBuffer map(File file, MapMode mode, long offset, long count) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file);
        N.checkArgNotNull(mode);
        N.checkArgNotNegative(offset, "offset");
        N.checkArgNotNegative(count, "count");

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
        String[] components = pathSplitter.splitToArray(pathname);
        List<String> path = new ArrayList<>();

        // resolve ., .., and //
        for (String component : components) {
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
    public static String getFileExtension(String fullName) throws IllegalArgumentException {
        N.checkArgNotNull(fullName);

        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
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
    public static String getNameWithoutExtension(String file) throws IllegalArgumentException {
        N.checkArgNotNull(file);

        String fileName = new File(file).getName();
        int dotIndex = fileName.lastIndexOf('.');
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
        } catch (FileNotFoundException e) {
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
        } catch (FileNotFoundException e) {
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
        } catch (FileNotFoundException e) {
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
        } catch (FileNotFoundException e) {
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
            return new FileReader(file); // NOSONAR
        } catch (FileNotFoundException e) {
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
        } catch (IOException e) {
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
            return new FileWriter(file); // NOSONAR
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        return new InputStreamReader(is); // NOSONAR
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
        return new OutputStreamWriter(os); // NOSONAR
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
    static java.io.BufferedReader newBufferedReader(String filePath) throws UncheckedIOException {
        return newBufferedReader(new File(filePath));
    }

    /**
     * New buffered reader.
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(File file) throws UncheckedIOException {
        return new java.io.BufferedReader(IOUtil.newFileReader(file));
    }

    /**
     * New buffered reader.
     *
     * @param file
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(File file, Charset charset) throws UncheckedIOException {
        return new java.io.BufferedReader(IOUtil.newInputStreamReader(IOUtil.newFileInputStream(file), checkCharset(charset)));
    }

    /**
     * New buffered reader.
     *
     * @param path
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(Path path) throws UncheckedIOException {
        try {
            return Files.newBufferedReader(path, DEFAULT_CHARSET);
        } catch (IOException e) {
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
    public static java.io.BufferedReader newBufferedReader(Path path, Charset charset) throws UncheckedIOException {
        try {
            return Files.newBufferedReader(path, charset);
        } catch (IOException e) {
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
    public static java.io.BufferedReader newBufferedReader(InputStream is) throws UncheckedIOException {
        return new java.io.BufferedReader(IOUtil.newInputStreamReader(is)); // NOSONAR
    }

    /**
     * New buffered reader.
     *
     * @param is
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedReader newBufferedReader(InputStream is, Charset charset) throws UncheckedIOException {
        return new java.io.BufferedReader(IOUtil.newInputStreamReader(is, checkCharset(charset)));
    }

    /**
     * New buffered writer.
     *
     * @param filePath
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    static java.io.BufferedWriter newBufferedWriter(String filePath) throws UncheckedIOException {
        return newBufferedWriter(new File(filePath));
    }

    /**
     * New buffered writer.
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedWriter newBufferedWriter(File file) throws UncheckedIOException {
        return new java.io.BufferedWriter(IOUtil.newFileWriter(file));
    }

    /**
     * New buffered writer.
     *
     * @param file
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedWriter newBufferedWriter(File file, Charset charset) throws UncheckedIOException {
        return new java.io.BufferedWriter(new OutputStreamWriter(IOUtil.newFileOutputStream(file), checkCharset(charset)));
    }

    /**
     * New buffered writer.
     *
     * @param os
     * @return
     */
    public static java.io.BufferedWriter newBufferedWriter(OutputStream os) {
        return new java.io.BufferedWriter(newOutputStreamWriter(os)); // NOSONAR
    }

    /**
     * New buffered writer.
     *
     * @param os
     * @param charset
     * @return
     */
    public static java.io.BufferedWriter newBufferedWriter(OutputStream os, Charset charset) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param closeable
     */
    public static void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw ExceptionUtil.toRuntimeException(e);
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

        for (AutoCloseable closeable : c) {
            try {
                close(closeable);
            } catch (Exception e) {
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
     *
     * @param closeable
     */
    public static void closeQuietly(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
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

        for (AutoCloseable closeable : c) {
            closeQuietly(closeable);
        }
    }

    /**
     *
     * @param srcFile
     * @param destDir
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void copyFileToDirectory(final File srcFile, final File destDir) throws UncheckedIOException {
        copyFileToDirectory(srcFile, destDir, true);
    }

    /**
     *
     * @param srcFile
     * @param destDir
     * @param preserveFileDate
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void copyFileToDirectory(final File srcFile, final File destDir, final boolean preserveFileDate) throws UncheckedIOException {
        copyFileToDirectory(srcFile, destDir, preserveFileDate, Fn.BiPredicates.alwaysTrue());
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
    public static <E extends Exception> void copyFileToDirectory(File srcFile, File destDir, final boolean preserveFileDate,
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
        } catch (IOException e) {
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
            } catch (IOException e) {
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
            } catch (IOException e) {
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

        for (File subFile : subFiles) {
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

            long size = input.size();
            long pos = 0;
            long count = 0;

            while (pos < size) {
                count = ((size - pos) > FILE_COPY_BUFFER_SIZE) ? FILE_COPY_BUFFER_SIZE : (size - pos);
                pos += output.transferFrom(input, pos, count);
            }
        } catch (IOException e) {
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
     * @see Files#copy(Path, Path, CopyOption...)
     */
    @SafeVarargs
    public static Path copy(Path source, Path target, CopyOption... options) throws UncheckedIOException {
        try {
            return Files.copy(source, target, options);
        } catch (IOException e) {
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
    public static long copy(InputStream in, Path target, CopyOption... options) throws UncheckedIOException {
        try {
            return Files.copy(in, target, options);
        } catch (IOException e) {
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
    public static long copy(Path source, OutputStream output) throws UncheckedIOException {
        try {
            return Files.copy(source, output);
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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

        File destFile = new File(destDir, srcFile.getName());

        if (!srcFile.renameTo(destFile)) {
            throw new UncheckedIOException(new IOException("Failed to move file from: " + srcFile.getAbsolutePath() + " to: " + destDir.getAbsolutePath()));
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
        return srcFile.renameTo(new File(srcFile.getParent() + IOUtil.FILE_SEPARATOR + newFileName));
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
    public static boolean deleteQuietly(File file) {
        try {
            return deleteIfExists(file);
        } catch (Throwable e) {
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
                for (File subFile : files) {
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
    public static <E extends Exception> boolean deleteFiles(final File file, Throwables.BiPredicate<? super File, ? super File, E> filter) throws E {
        if ((file == null) || !file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            final File[] files = file.listFiles();

            if (N.isEmpty(files)) {
                return true;
            }

            for (File subFile : files) {
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
        if (!file.exists()) { //NOSONAR
            if (file.createNewFile() == false) { //NOSONAR
                throw new IOException("Failed to create new file: " + file.getName());
            }
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
        } catch (IOException e) {
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
        N.checkArgNotNull(file, "file");
        N.checkArgNotNull(date, "date");

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
        N.checkArgNotNull(file, "file");
        N.checkArgNotNull(reference, "reference");

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
        N.checkArgNotNull(file, "file");
        N.checkArgNotNull(date, "date");

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
        N.checkArgNotNull(file, "file");
        N.checkArgNotNull(reference, "reference");

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
        N.checkArgNotNull(file, "file");

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
        N.checkArgNotNull(file, "file");

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
        N.checkArgNotNull(directory, "directory");

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
        } catch (IOException e) {
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

            for (File sourceFile : sourceFiles) {
                zipFile(sourceFile, zos, targetFile);
            }
        } catch (IOException e) {
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
            List<File> subFileList = listFiles(sourceFile, true, true);

            // subFileList.add(sourceFile);
            for (File subFile : subFileList) {
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

        ZipEntry ze = null;
        String relativeFileName = null;

        if (sourceDir == null) {
            relativeFileName = file.getName();
        } else {
            relativeFileName = getRelativePath(sourceDir, file);
        }

        ze = new ZipEntry(relativeFileName);
        ze.setSize(file.length());
        ze.setTime(file.lastModified());
        zos.putNextEntry(ze);

        InputStream is = IOUtil.newFileInputStream(file);

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

            Enumeration<? extends ZipEntry> entryEnum = zip.entries();

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
        } catch (IOException e) {
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
                String subFileNmae = destDir.getAbsolutePath() + IOUtil.FILE_SEPARATOR + fileName + "_" + Strings.padStart(N.stringOf(fileSerNum++), 4, '0');
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
        } catch (IOException e) {
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
        N.checkArgPositive(numOfParts, "numOfParts");

        final int suffixLen = String.valueOf(numOfParts).length();

        final long lineNumOfPart = N.max(estimateLineCount(file, 10000) / numOfParts, 1);

        int index = file.getName().lastIndexOf('.');
        String prefix = file.getName().substring(0, index);
        String postfix = (index > 0) ? file.getName().substring(index) : "";

        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        BufferedReader br = null;
        BufferedWriter bw = null;
        int fileSerNum = 1;

        try {
            is = openFile(file, outputZipFile);

            br = Objectory.createBufferedReader(is);

            String subFileNmae = destDir.getAbsolutePath() + IOUtil.FILE_SEPARATOR + prefix + "_" + Strings.padStart(N.stringOf(fileSerNum++), suffixLen, '0')
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

                    subFileNmae = destDir.getAbsolutePath() + IOUtil.FILE_SEPARATOR + prefix + "_" + Strings.padStart(N.stringOf(fileSerNum++), suffixLen, '0')
                            + postfix;
                    bw = Objectory.createBufferedWriter(IOUtil.newFileWriter(new File(subFileNmae)));
                }
            }

            close(bw);
            Objectory.recycle(bw);
            bw = null;
        } catch (IOException e) {
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
        } catch (IOException e) {
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
        final byte[] buf = Objectory.createByteArrayBuffer();

        long totalCount = 0;
        OutputStream output = null;

        try {
            output = IOUtil.newFileOutputStream(destFile);

            InputStream input = null;
            for (File file : sourceFiles) {
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
        } catch (IOException e) {
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
            char c = relativeFilePath.charAt(i);

            if ((c == '\\') || (c == '/')) {
                newRelativePath += File.separator; //NOSONAR
            } else {
                newRelativePath += c; //NOSONAR
            }
        }

        relativeFilePath = newRelativePath;

        String path = parentDir.getAbsolutePath() + File.separator + relativeFilePath;

        File dir = new File(path.substring(0, path.lastIndexOf(File.separator)));

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
    public static List<String> list(File parentPath, final boolean recursively, final boolean excludeDirectory) {
        return list(parentPath, recursively, excludeDirectory ? directories_excluded_filter : all_files_filter);
    }

    /**
     *
     * @param <E>
     * @param parentPath
     * @param recursively
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> List<String> list(File parentPath, final boolean recursively,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws E {
        List<String> files = new ArrayList<>();

        if (!parentPath.exists()) {
            return files;
        }

        parentPath = new File(parentPath.getAbsolutePath().replace(".\\", "\\").replace("./", "/"));

        File[] subFiles = parentPath.listFiles();

        if (N.isEmpty(subFiles)) {
            return files;
        }

        for (File file : subFiles) {
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
     *
     * @param <E>
     * @param parentPath
     * @param recursively
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> List<File> listFiles(final File parentPath, final boolean recursively,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws E {
        final List<File> files = new ArrayList<>();

        if (!parentPath.exists()) {
            return files;
        }

        File[] subFiles = parentPath.listFiles();

        if (N.isEmpty(subFiles)) {
            return files;
        }

        for (File file : subFiles) {
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

        for (URL url : urls) {
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
        } catch (IOException e) {
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
        } catch (IOException e) {
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
            for (File file : files) {
                urls.add(file.toURI().toURL());
            }
        } catch (IOException e) {
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
     *
     * @param <E>
     * @param source
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final File source, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final File source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
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
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
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
     */
    public static <E extends Exception> void parse(final File source, final long lineOffset, final long count, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified files/directory line by line.
     *
     * @param <E>
     * @param <E2>
     * @param source parse all the sub files recursively if the element is a directory.
     * @param lineOffset
     * @param count
     * @param processThreadNum new threads started to parse/process the lines/records
     * @param queueSize
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(source.isDirectory() ? listFiles(source, true, true) : Array.asList(source), lineOffset, count, processThreadNum, queueSize, lineParser,
                onComplete);
    }

    /**
     *
     * @param <E>
     * @param files
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Collection<File> files, final Throwables.Consumer<? super String, E> lineParser)
            throws UncheckedIOException, E {
        parse(files, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param files
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Collection<File> files, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(files, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param files
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
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
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(files, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
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
     */
    public static <E extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified files/directory line by line.
     *
     * @param <E>
     * @param <E2>
     * @param files parse all the sub files recursively if the element is a directory.
     * @param lineOffset
     * @param count
     * @param processThreadNum thread number used to parse/process the lines/records
     * @param queueSize size of queue to save the processing records/lines loaded from source data. Default size is 1024.
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
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

            for (Reader reader : readers) {
                iterators.add(new LineIterator(reader));
            }

            Iterators.forEach(iterators, lineOffset, count, 0, processThreadNum, queueSize, lineParser, onComplete);
        } finally {
            for (Reader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    /**
     *
     * @param <E>
     * @param source
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final File source, final int readThreadNum, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
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
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final int readThreadNum, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(source, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
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
     */
    public static <E extends Exception> void parse(final File source, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified files/directory line by line.
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
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File source, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(source.isDirectory() ? listFiles(source, true, true) : Array.asList(source), lineOffset, count, readThreadNum, processThreadNum, queueSize,
                lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param files
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Collection<File> files, final int readThreadNum, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
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
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Collection<File> files, final int readThreadNum, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(files, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
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
     */
    public static <E extends Exception> void parse(final Collection<File> files, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(files, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified files/directory line by line.
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

            for (Reader reader : readers) {
                iterators.add(new LineIterator(reader));
            }

            Iterators.forEach(iterators, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser, onComplete);
        } finally {
            for (Reader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    /**
     *
     * @param <E>
     * @param source
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final InputStream source, final Throwables.Consumer<? super String, E> lineParser)
            throws UncheckedIOException, E {
        parse(source, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final InputStream source, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final InputStream source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
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
     */
    public static <E extends Exception, E2 extends Exception> void parse(final InputStream source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
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
     * @throws E2 the e2
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
     *
     * @param <E>
     * @param source
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Reader source, final Throwables.Consumer<? super String, E> lineParser)
            throws UncheckedIOException, E {
        parse(source, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param source
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Reader source, final Throwables.Consumer<? super String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param source
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Reader source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser) throws UncheckedIOException, E {
        parse(source, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
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
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Reader source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(source, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
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
            ZipFile zf = new ZipFile(source);

            ZipEntry ze = zf.entries().nextElement();
            is = zf.getInputStream(ze);
            outputZipFile.setValue(zf);
        } else {
            is = IOUtil.newFileInputStream(source);
        }

        return is;
    }

    /**
     * To char array.
     *
     * @param str
     * @return
     */
    @SuppressWarnings("deprecation")
    private static char[] toCharArray(CharSequence str) {
        return str == null ? Strings.NULL_CHAR_ARRAY : InternalUtil.getCharsForReadOnly(str instanceof String ? (String) str : str.toString());
    }
}
