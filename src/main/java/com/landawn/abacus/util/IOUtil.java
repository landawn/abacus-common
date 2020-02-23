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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
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
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.function.BiPredicate;

/**
 * The Class IOUtil.
 *
 * @author Haiyang Li
 * @version $Revision: 0.8 $
 */
public final class IOUtil {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(IOUtil.class);

    /** The Constant pathSplitter. */
    private static final Splitter pathSplitter = Splitter.with('/').trim(true);

    /** The Constant DEFAULT_QUEUE_SIZE_FOR_ROW_PARSER. */
    static final int DEFAULT_QUEUE_SIZE_FOR_ROW_PARSER = 1024;

    /** The Constant ZIP. */
    // ...
    private static final String ZIP = ".zip";

    /** The Constant GZ. */
    private static final String GZ = ".gz";

    /** The file copy buffer size (32 MB). */
    private static final long FILE_COPY_BUFFER_SIZE = (1024 * 1024) * 32;

    /** The Constant stringEncodeMethod. */
    static final Method stringEncodeMethod;

    /** The Constant stringDecodeMethod. */
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
                byte[] bytes = ClassUtil.invokeMethod(enMethod, Charsets.UTF_8, chars, 1, 1);
                char[] chars2 = ClassUtil.invokeMethod(deMethod, Charsets.UTF_8, bytes, 0, bytes.length);

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

    /** The Constant HOST_NAME. */
    public static final String HOST_NAME;

    static {
        String hostName = null;
        final boolean IS_PLATFORM_ANDROID = System.getProperty("java.vendor").toUpperCase().contains("ANDROID")
                || System.getProperty("java.vm.vendor").toUpperCase().contains("ANDROID");

        // implementation for android support
        if (IS_PLATFORM_ANDROID) {
            try {
                hostName = Executors.newSingleThreadExecutor().submit(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return InetAddress.getLocalHost().getHostName();
                    }
                }).get();
            } catch (Exception e) {
                logger.error("Failed to get host name");
            }
        } else {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                logger.error("Failed to get host name");
            }
        }

        HOST_NAME = hostName;
    }

    /** The Constant CPU_CORES. */
    public static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /** The Constant ONE_KB. */
    public static final long ONE_KB = 1024;

    /** The Constant ONE_MB. */
    public static final long ONE_MB = 1024 * ONE_KB;

    /** The Constant ONE_GB. */
    public static final long ONE_GB = 1024 * ONE_MB;

    /** The Constant ONE_TB. */
    public static final long ONE_TB = 1024 * ONE_GB;

    /** The Constant ONE_PB. */
    public static final long ONE_PB = 1024 * ONE_TB;

    /** The Constant ONE_EB. */
    public static final long ONE_EB = 1024 * ONE_PB;

    /** The Constant ONE_ZB. */
    public static final long ONE_ZB = 1024 * ONE_EB;

    /** The Constant MAX_MEMORY_IN_MB. */
    public static final int MAX_MEMORY_IN_MB = (int) (Runtime.getRuntime().maxMemory() / (1024 * 1024));

    /** The Constant OS_NAME. */
    // ...
    public static final String OS_NAME = System.getProperty("os.name");

    /** The Constant OS_VERSION. */
    public static final String OS_VERSION = System.getProperty("os.version");

    /** The Constant OS_ARCH. */
    public static final String OS_ARCH = System.getProperty("os.arch");

    /** The Constant IS_OS_WINDOWS. */
    //...
    public static final boolean IS_OS_WINDOWS = OS_NAME.toUpperCase().contains("WINDOWS");

    /** The Constant IS_OS_MAC. */
    public static final boolean IS_OS_MAC = OS_NAME.toUpperCase().contains("MAC");

    /** The Constant IS_OS_MAC_OSX. */
    public static final boolean IS_OS_MAC_OSX = OS_NAME.toUpperCase().contains("MAC OS X");

    /** The Constant IS_OS_LINUX. */
    public static final boolean IS_OS_LINUX = OS_NAME.toUpperCase().contains("LINUX");

    /** The Constant IS_PLATFORM_ANDROID. */
    public static final boolean IS_PLATFORM_ANDROID = System.getProperty("java.vendor").toUpperCase().contains("ANDROID")
            || System.getProperty("java.vm.vendor").toUpperCase().contains("ANDROID");

    /** The Constant JAVA_HOME. */
    // ...
    public static final String JAVA_HOME = System.getProperty("java.home");

    /** The Constant JAVA_VERSION. */
    public static final String JAVA_VERSION = System.getProperty("java.version");

    /** The Constant JAVA_VENDOR. */
    public static final String JAVA_VENDOR = System.getProperty("java.vendor");

    /** The Constant JAVA_CLASS_PATH. */
    public static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");

    /** The Constant JAVA_CLASS_VERSION. */
    public static final String JAVA_CLASS_VERSION = System.getProperty("java.class.version");

    /** The Constant JAVA_RUNTIME_NAME. */
    public static final String JAVA_RUNTIME_NAME = System.getProperty("java.runtime.name");

    /** The Constant JAVA_RUNTIME_VERSION. */
    public static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");

    /** The Constant JAVA_SPECIFICATION_NAME. */
    public static final String JAVA_SPECIFICATION_NAME = System.getProperty("java.specification.name");

    /** The Constant JAVA_SPECIFICATION_VENDOR. */
    public static final String JAVA_SPECIFICATION_VENDOR = System.getProperty("java.specification.vendor");

    /** The Constant JAVA_SPECIFICATION_VERSION. */
    public static final String JAVA_SPECIFICATION_VERSION = System.getProperty("java.specification.version");

    /** The Constant JAVA_VM_INFO. */
    public static final String JAVA_VM_INFO = System.getProperty("java.vm.info");

    /** The Constant JAVA_VM_NAME. */
    public static final String JAVA_VM_NAME = System.getProperty("java.vm.name");

    /** The Constant JAVA_VM_SPECIFICATION_NAME. */
    public static final String JAVA_VM_SPECIFICATION_NAME = System.getProperty("java.vm.specification.name");

    /** The Constant JAVA_VM_SPECIFICATION_VENDOR. */
    public static final String JAVA_VM_SPECIFICATION_VENDOR = System.getProperty("java.vm.specification.vendor");

    /** The Constant JAVA_VM_SPECIFICATION_VERSION. */
    public static final String JAVA_VM_SPECIFICATION_VERSION = System.getProperty("java.vm.specification.version");

    /** The Constant JAVA_VM_VENDOR. */
    public static final String JAVA_VM_VENDOR = System.getProperty("java.vm.vendor");

    /** The Constant JAVA_VM_VERSION. */
    public static final String JAVA_VM_VERSION = System.getProperty("java.vm.version");

    /** The Constant JAVA_IO_TMPDIR. */
    public static final String JAVA_IO_TMPDIR = System.getProperty("java.io.tmpdir");

    /** The Constant JAVA_VENDOR_URL. */
    static final String JAVA_VENDOR_URL = System.getProperty("java.vendor.url");

    /** The Constant JAVA_LIBRARY_PATH. */
    static final String JAVA_LIBRARY_PATH = System.getProperty("java.library.path");

    /** The Constant JAVA_COMPILER. */
    static final String JAVA_COMPILER = System.getProperty("java.compiler");

    /** The Constant JAVA_ENDORSED_DIRS. */
    static final String JAVA_ENDORSED_DIRS = System.getProperty("java.endorsed.dirs");

    /** The Constant JAVA_EXT_DIRS. */
    static final String JAVA_EXT_DIRS = System.getProperty("java.ext.dirs");

    /** The Constant JAVA_AWT_FONTS. */
    // ...
    static final String JAVA_AWT_FONTS = System.getProperty("java.awt.fonts");

    /** The Constant JAVA_AWT_GRAPHICSENV. */
    static final String JAVA_AWT_GRAPHICSENV = System.getProperty("java.awt.graphicsenv");

    /** The Constant JAVA_AWT_HEADLESS. */
    static final String JAVA_AWT_HEADLESS = System.getProperty("java.awt.headless");

    /** The Constant JAVA_AWT_PRINTERJOB. */
    static final String JAVA_AWT_PRINTERJOB = System.getProperty("java.awt.printerjob");

    /** The Constant JAVA_UTIL_PREFS_PREFERENCES_FACTORY. */
    static final String JAVA_UTIL_PREFS_PREFERENCES_FACTORY = System.getProperty("java.util.prefs.PreferencesFactory");

    /** The Constant USER_DIR. */
    // ...
    public static final String USER_DIR = System.getProperty("user.dir");

    /** The Constant USER_HOME. */
    public static final String USER_HOME = System.getProperty("user.home");

    /** The Constant USER_NAME. */
    public static final String USER_NAME = System.getProperty("user.name");

    /** The Constant USER_TIMEZONE. */
    public static final String USER_TIMEZONE = System.getProperty("user.timezone");

    /** The Constant USER_LANGUAGE. */
    public static final String USER_LANGUAGE = System.getProperty("user.language");

    /** The Constant USER_COUNTRY. */
    public static final String USER_COUNTRY = System.getProperty("user.country") == null ? System.getProperty("user.region")
            : System.getProperty("user.country");

    /** current path retrieved by {@code new File("./").getAbsolutePath()} */
    public static final String CURRENT_PATH;

    static {
        final String path = new File("./").getAbsolutePath();
        CURRENT_PATH = path.charAt(path.length() - 1) == '.' ? path.substring(0, path.length() - 1) : path;
    }

    /** The Constant PATH_SEPARATOR. */
    // ...
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");

    /** The Constant FILE_SEPARATOR. */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /** The Constant LINE_SEPARATOR. */
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /** The Constant EOF. */
    // ...
    public static final int EOF = -1;

    /** The Constant all_files_filter. */
    private static final BiPredicate<File, File> all_files_filter = new BiPredicate<File, File>() {
        @Override
        public boolean test(File parentDir, File file) {
            return true;
        }
    };

    /** The Constant directories_excluded_filter. */
    private static final BiPredicate<File, File> directories_excluded_filter = new BiPredicate<File, File>() {
        @Override
        public boolean test(File parentDir, File file) {
            return !file.isDirectory();
        }
    };

    /** The Constant directories_only_filter. */
    private static final BiPredicate<File, File> directories_only_filter = new BiPredicate<File, File>() {
        @Override
        public boolean test(File parentDir, File file) {
            return file.isDirectory();
        }
    };

    /**
     * Constructor for FileUtil.
     */
    private IOUtil() {
        // no instance;
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

    /**
     * Chars 2 bytes.
     *
     * @param chars
     * @return
     */
    public static byte[] chars2Bytes(final char[] chars) {
        return chars2Bytes(chars, Charsets.UTF_8);
    }

    /**
     * Chars 2 bytes.
     *
     * @param chars
     * @param charset
     * @return
     */
    public static byte[] chars2Bytes(final char[] chars, final Charset charset) {
        if (N.isNullOrEmpty(chars)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return chars2Bytes(chars, 0, chars.length, charset);
    }

    /**
     * Chars 2 bytes.
     *
     * @param chars
     * @param offset
     * @param len
     * @param charset
     * @return
     */
    public static byte[] chars2Bytes(final char[] chars, final int offset, final int len, Charset charset) {
        if (len == 0 && N.len(chars) >= offset) {
            return N.EMPTY_BYTE_ARRAY;
        }

        charset = charset == null ? Charsets.UTF_8 : charset;

        if (stringEncodeMethod == null) {
            return new String(chars, offset, len).getBytes(charset);
        } else {
            return ClassUtil.invokeMethod(stringEncodeMethod, charset, chars, offset, len);
        }
    }

    /**
     * Bytes 2 chars.
     *
     * @param bytes
     * @return
     */
    public static char[] bytes2Chars(final byte[] bytes) {
        return bytes2Chars(bytes, Charsets.UTF_8);
    }

    /**
     * Bytes 2 chars.
     *
     * @param bytes
     * @param charset
     * @return
     */
    public static char[] bytes2Chars(final byte[] bytes, final Charset charset) {
        if (N.isNullOrEmpty(bytes)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return bytes2Chars(bytes, 0, bytes.length, charset);
    }

    /**
     * Bytes 2 chars.
     *
     * @param bytes
     * @param offset
     * @param len
     * @param charset
     * @return
     */
    public static char[] bytes2Chars(final byte bytes[], final int offset, final int len, Charset charset) {
        if (len == 0 && N.len(bytes) >= offset) {
            return N.EMPTY_CHAR_ARRAY;
        }

        charset = charset == null ? Charsets.UTF_8 : charset;

        if (stringDecodeMethod == null) {
            return new String(bytes, offset, len, charset).toCharArray();
        } else {
            return ClassUtil.invokeMethod(stringDecodeMethod, charset, bytes, offset, len);
        }
    }

    /**
     * String 2 input stream.
     *
     * @param str
     * @return
     */
    public static InputStream string2InputStream(final String str) {
        return string2InputStream(str, Charsets.UTF_8);
    }

    /**
     * String 2 input stream.
     *
     * @param str
     * @param charset
     * @return
     */
    public static InputStream string2InputStream(final String str, Charset charset) {
        if (str == null) {
            throw new IllegalArgumentException("The input String can't be null.");
        }

        charset = charset == null ? Charsets.UTF_8 : charset;

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
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static byte[] readAllBytes(final File file) throws UncheckedIOException {
        return readBytes(file, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static byte[] readAllBytes(final InputStream is) throws UncheckedIOException {
        return readBytes(is, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllBytes(File)}.
     * @see #readAllBytes(File)
     */
    @Deprecated
    public static byte[] readBytes(final File file) throws UncheckedIOException {
        return readBytes(file, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param file
     * @param offset
     * @param maxLen
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static byte[] readBytes(final File file, final long offset, final int maxLen) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(outputZipFile, file);

            return readBytes(is, offset, maxLen);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllBytes(InputStream)}.
     * @see #readAllBytes(InputStream)
     */
    @Deprecated
    public static byte[] readBytes(final InputStream is) throws UncheckedIOException {
        return readBytes(is, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param is
     * @param offset
     * @param maxLen
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static byte[] readBytes(final InputStream is, final long offset, final int maxLen) throws UncheckedIOException {
        if (maxLen == 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        if (offset > 0) {
            if (skip(is, offset) < offset) {
                return N.EMPTY_BYTE_ARRAY;
            }
        }

        ByteArrayOutputStream os = null;
        final byte[] buf = Objectory.createByteArrayBuffer();
        final int bufLength = buf.length;

        int totalCount = 0;
        int count = 0;

        try {
            while (totalCount < maxLen && EOF != (count = read(is, buf, 0, Math.min(maxLen - totalCount, bufLength)))) {
                if (count == bufLength && count < maxLen) {
                    if (os == null) {
                        os = Objectory.createByteArrayOutputStream();
                    }
                }

                if (os != null) {
                    os.write(buf, 0, count);
                }

                totalCount += count;
            }

            return os == null ? (totalCount <= 0 ? N.EMPTY_BYTE_ARRAY : N.copyOfRange(buf, 0, totalCount)) : os.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);
            Objectory.recycle(os);
        }
    }

    /**
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final File file) throws UncheckedIOException {
        return readChars(file, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param file
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final File file, final Charset encoding) throws UncheckedIOException {
        return readChars(file, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final InputStream is) throws UncheckedIOException {
        return readChars(is, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param is
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final InputStream is, final Charset encoding) throws UncheckedIOException {
        return readChars(is, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param reader
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readAllChars(final Reader reader) throws UncheckedIOException {
        return readChars(reader, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllChars}
     * @see #readAllChars(File)
     */
    @Deprecated
    public static char[] readChars(final File file) throws UncheckedIOException {
        return readChars(file, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param file
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllChars}
     * @see #readAllChars(File, Charset)
     */
    @Deprecated
    public static char[] readChars(final File file, final Charset encoding) throws UncheckedIOException {
        return readChars(file, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param file
     * @param offset
     * @param maxLen
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readChars(final File file, final long offset, final int maxLen) throws UncheckedIOException {
        return readChars(file, 0, maxLen, Charsets.UTF_8);
    }

    /**
     *
     * @param file
     * @param offset
     * @param maxLen
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readChars(final File file, final long offset, final int maxLen, final Charset encoding) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(outputZipFile, file);

            return readChars(is, offset, maxLen, encoding);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllChars}
     * @see #readAllChars(InputStream)
     */
    @Deprecated
    public static char[] readChars(final InputStream is) throws UncheckedIOException {
        return readChars(is, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param is
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllChars}
     * @see #readAllChars(InputStream, encoding)
     */
    @Deprecated
    public static char[] readChars(final InputStream is, final Charset encoding) throws UncheckedIOException {
        return readChars(is, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param is
     * @param offset
     * @param maxLen
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readChars(final InputStream is, final long offset, final int maxLen) throws UncheckedIOException {
        return readChars(is, 0, maxLen, Charsets.UTF_8);
    }

    /**
     *
     * @param is
     * @param offset
     * @param maxLen
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readChars(final InputStream is, final long offset, final int maxLen, Charset encoding) throws UncheckedIOException {
        encoding = encoding == null ? Charsets.UTF_8 : encoding;

        Reader reader = null;

        // try {
        reader = createReader(is, encoding);

        return readChars(reader, offset, maxLen);

        // } finally {
        // // close(reader);
        // }
    }

    /**
     *
     * @param reader
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllChars}
     * @see #readAllChars(Reader)
     */
    @Deprecated
    public static char[] readChars(final Reader reader) throws UncheckedIOException {
        return readChars(reader, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param reader
     * @param offset
     * @param maxLen
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static char[] readChars(final Reader reader, final long offset, final int maxLen) throws UncheckedIOException {
        if (maxLen == 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        if (offset > 0) {
            if (skip(reader, offset) < offset) {
                return N.EMPTY_CHAR_ARRAY;
            }
        }

        StringBuilder sb = null;
        final char[] buf = Objectory.createCharArrayBuffer();
        final int bufLength = buf.length;

        int totalCount = 0;
        int count = 0;

        try {
            while (totalCount < maxLen && EOF != (count = read(reader, buf, 0, Math.min(maxLen - totalCount, bufLength)))) {
                if (count == bufLength && count < maxLen) {
                    if (sb == null) {
                        sb = Objectory.createStringBuilder();
                    }
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);
            Objectory.recycle(sb);
        }
    }

    /**
     * Read all content into one {@code String}.
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readString(final File file) throws UncheckedIOException {
        return readString(file, 0, Integer.MAX_VALUE);
    }

    /**
     * Read all content into one {@code String}.
     *
     * @param file
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readString(final File file, final Charset encoding) throws UncheckedIOException {
        return readString(file, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param file
     * @param offset
     * @param maxLen
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readString(final File file, final long offset, final int maxLen) throws UncheckedIOException {
        return readString(file, offset, maxLen, Charsets.UTF_8);
    }

    /**
     *
     * @param file
     * @param offset
     * @param maxLen
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("deprecation")
    public static String readString(final File file, final long offset, final int maxLen, final Charset encoding) throws UncheckedIOException {
        final char[] chs = readChars(file, offset, maxLen, encoding);

        return N.isNullOrEmpty(chs) ? N.EMPTY_STRING : InternalUtil.newString(chs, true);
    }

    /**
     * Read all content into one {@code String}.
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readString(final InputStream is) throws UncheckedIOException {
        return readString(is, 0, Integer.MAX_VALUE);
    }

    /**
     * Read all content into one {@code String}.
     *
     * @param is
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readString(final InputStream is, final Charset encoding) throws UncheckedIOException {
        return readString(is, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param is
     * @param offset
     * @param maxLen
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readString(final InputStream is, final long offset, final int maxLen) throws UncheckedIOException {
        return readString(is, offset, maxLen, Charsets.UTF_8);
    }

    /**
     *
     * @param is
     * @param offset
     * @param maxLen
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("deprecation")
    public static String readString(final InputStream is, final long offset, final int maxLen, final Charset encoding) throws UncheckedIOException {
        final char[] chs = readChars(is, offset, maxLen, encoding);

        return N.isNullOrEmpty(chs) ? N.EMPTY_STRING : InternalUtil.newString(chs, true);
    }

    /**
     * Read all content into one {@code String}.
     *
     * @param reader
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readString(final Reader reader) throws UncheckedIOException {
        return readString(reader, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param reader
     * @param offset
     * @param maxLen
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    @SuppressWarnings("deprecation")
    public static String readString(final Reader reader, final long offset, final int maxLen) throws UncheckedIOException {
        final char[] chs = readChars(reader, offset, maxLen);

        return N.isNullOrEmpty(chs) ? N.EMPTY_STRING : InternalUtil.newString(chs, true);
    }

    /**
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readLine(final File file) throws UncheckedIOException {
        return readLine(file, 0);
    }

    /**
     *
     * @param file
     * @param lineIndex
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readLine(final File file, final int lineIndex) throws UncheckedIOException {
        return readLine(file, lineIndex, Charsets.UTF_8);
    }

    /**
     *
     * @param file
     * @param lineIndex
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readLine(final File file, final int lineIndex, final Charset encoding) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(outputZipFile, file);

            return readLine(is, lineIndex, encoding);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readLine(final InputStream is) throws UncheckedIOException {
        return readLine(is, 0);
    }

    /**
     *
     * @param is
     * @param lineIndex
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readLine(final InputStream is, final int lineIndex) throws UncheckedIOException {
        return readLine(is, lineIndex, Charsets.UTF_8);
    }

    /**
     *
     * @param is
     * @param lineIndex
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readLine(final InputStream is, final int lineIndex, final Charset encoding) throws UncheckedIOException {
        return readLine(createReader(is, encoding), lineIndex);
    }

    /**
     *
     * @param reader
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readLine(final Reader reader) throws UncheckedIOException {
        return readLine(reader, 0);
    }

    /**
     *
     * @param reader
     * @param lineIndex
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static String readLine(final Reader reader, int lineIndex) throws UncheckedIOException {
        final BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : Objectory.createBufferedReader(reader);

        try {
            if (lineIndex == 0) {
                return br.readLine();
            } else {
                while (lineIndex-- > 0 && br.readLine() != null) {
                }

                return br.readLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != reader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final File file) throws UncheckedIOException {
        return readLines(file, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param file
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final File file, final Charset encoding) throws UncheckedIOException {
        return readLines(file, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final InputStream is) throws UncheckedIOException {
        return readLines(is, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param is
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final InputStream is, final Charset encoding) throws UncheckedIOException {
        return readLines(is, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param reader
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readAllLines(final Reader reader) throws UncheckedIOException {
        return readLines(reader, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param file
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllLines}
     * @see #readAllLines(File)
     */
    @Deprecated
    public static List<String> readLines(final File file) throws UncheckedIOException {
        return readAllLines(file);
    }

    /**
     *
     * @param file
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllLines}
     * @see #readAllLines(File, Charset)
     */
    @Deprecated
    public static List<String> readLines(final File file, final Charset encoding) throws UncheckedIOException {
        return readAllLines(file, encoding);
    }

    /**
     *
     * @param file
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readLines(final File file, final int offset, final int count) throws UncheckedIOException {
        return readLines(file, offset, count, Charsets.UTF_8);
    }

    /**
     *
     * @param file
     * @param offset
     * @param count
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readLines(final File file, final int offset, final int count, final Charset encoding) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(outputZipFile, file);

            return readLines(is, offset, count, encoding);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllLines}
     * @see #readAllLines(is)
     */
    @Deprecated
    public static List<String> readLines(final InputStream is) throws UncheckedIOException {
        return readLines(is, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param is
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllLines}
     * @see #readAllLines(is, Charset)
     */
    @Deprecated
    public static List<String> readLines(final InputStream is, final Charset encoding) throws UncheckedIOException {
        return readLines(is, 0, Integer.MAX_VALUE, encoding);
    }

    /**
     *
     * @param is
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readLines(final InputStream is, final int offset, final int count) throws UncheckedIOException {
        return readLines(is, offset, count, Charsets.UTF_8);
    }

    /**
     *
     * @param is
     * @param offset
     * @param count
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readLines(final InputStream is, final int offset, final int count, final Charset encoding) throws UncheckedIOException {
        return readLines(createReader(is, encoding), offset, count);
    }

    /**
     * Creates the reader.
     *
     * @param is
     * @param encoding
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    private static InputStreamReader createReader(final InputStream is, final Charset encoding) throws UncheckedIOException {
        return encoding == null ? new InputStreamReader(is, Charsets.UTF_8) : new InputStreamReader(is, encoding);
    }

    /**
     *
     * @param reader
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @deprecated replaced by {@code readAllLines}
     * @see #readAllLines(Reader)
     */
    @Deprecated
    public static List<String> readLines(final Reader reader) throws UncheckedIOException {
        return readLines(reader, 0, Integer.MAX_VALUE);
    }

    /**
     *
     * @param reader
     * @param offset
     * @param count
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static List<String> readLines(final Reader reader, int offset, int count) throws UncheckedIOException {
        final List<String> res = new ArrayList<>();
        final BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : Objectory.createBufferedReader(reader);

        try {
            while (offset-- > 0 && br.readLine() != null) {
            }

            String line = null;

            while (count-- > 0 && (line = br.readLine()) != null) {
                res.add(line);
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (br != reader) {
                Objectory.recycle(br);
            }
        }

        return res;
    }

    /**
     * Returns an Iterator for the lines in a <code>File</code> using the default encoding for the VM.
     *
     * @param file the file to open for input, must not be {@code null}
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws UncheckedIOException in case of an I/O error (file closed)
     * @see #iterate(File, Charset)
     */
    public static LineIterator iterate(final File file) throws UncheckedIOException {
        return iterate(file, Charsets.UTF_8);
    }

    /**
     * Returns an Iterator for the lines in a <code>File</code>.
     * <p>
     * This method opens an <code>InputStream</code> for the file.
     * When you have finished with the iterator you should close the stream
     * to free internal resources. This can be done by calling the
     * {@link LineIterator#close()} or
     * {@link IOUtil#closeQuietly(LineIterator)} method.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * LineIterator it = FileUtils.iterate(file, "UTF-8");
     * try {
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   closeQuietly(iterator);
     * }
     * </pre>
     * <p>
     * If an exception occurs during the creation of the iterator, the
     * underlying stream is closed.
     *
     * @param file the file to open for input, must not be {@code null}
     * @param encoding the encoding to use, {@code null} means platform default
     * @return an Iterator of the lines in the file, never {@code null}
     * @throws UncheckedIOException in case of an I/O error (file closed)
     */
    public static LineIterator iterate(final File file, final Charset encoding) throws UncheckedIOException {
        InputStream in = null;

        try {
            in = new FileInputStream(file);

            return iterate(in, encoding);
        } catch (final IOException ex) {
            closeQuietly(in);
            throw new UncheckedIOException(ex);
        } catch (final RuntimeException ex) {
            closeQuietly(in);
            throw ex;
        }
    }

    /**
     *
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static LineIterator iterate(final InputStream input) throws UncheckedIOException {
        return iterate(input, null);
    }

    /**
     * Returns an Iterator for the lines in an <code>InputStream</code>, using
     * the character encoding specified (or default encoding if null).
     * <p>
     * <code>LineIterator</code> holds a reference to the open
     * <code>InputStream</code> specified here. When you have finished with
     * the iterator you should close the stream to free internal resources.
     * This can be done by closing the stream directly, or by calling
     * {@link LineIterator#close()} or {@link IOUtil#closeQuietly(LineIterator)}.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * try {
     *   LineIterator it = iterate(stream, charset);
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   closeQuietly(stream);
     * }
     * </pre>
     *
     * @param input the <code>InputStream</code> to read from, not null
     * @param encoding the encoding to use, null means platform default
     * @return an Iterator of the lines in the reader, never null
     * @throws UncheckedIOException if an I/O error occurs, such as if the encoding is invalid
     * @throws IllegalArgumentException if the input is null
     */
    public static LineIterator iterate(final InputStream input, final Charset encoding) throws UncheckedIOException {
        return new LineIterator(createReader(input, encoding));
    }

    // lineIterator
    //-----------------------------------------------------------------------
    /**
     * Returns an Iterator for the lines in a <code>Reader</code>.
     * <p>
     * <code>LineIterator</code> holds a reference to the open
     * <code>Reader</code> specified here. When you have finished with the
     * iterator you should close the reader to free internal resources.
     * This can be done by closing the reader directly, or by calling
     * {@link LineIterator#close()} or {@link IOUtil#closeQuietly(LineIterator)}.
     * <p>
     * The recommended usage pattern is:
     * <pre>
     * try {
     *   LineIterator it = iterate(reader);
     *   while (it.hasNext()) {
     *     String line = it.nextLine();
     *     /// do something with line
     *   }
     * } finally {
     *   closeQuietly(reader);
     * }
     * </pre>
     *
     * @param reader the <code>Reader</code> to read from, not null
     * @return an Iterator of the lines in the reader, never null
     * @throws UncheckedIOException the unchecked IO exception
     * @throws IllegalArgumentException if the reader is null
     */
    public static LineIterator iterate(final Reader reader) throws UncheckedIOException {
        return new LineIterator(reader);
    }

    /**
     *
     * @param file
     * @param buf
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static int read(final File file, final byte[] buf) throws UncheckedIOException {
        return read(file, buf, 0, buf.length);
    }

    /**
     *
     * @param file
     * @param buf
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static int read(final File file, final byte[] buf, final int off, final int len) throws UncheckedIOException {
        InputStream is = null;

        try {
            is = new FileInputStream(file);
            return read(is, buf, off, len);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeQuietly(is);
        }
    }

    /**
     *
     * @param is
     * @param buf
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static int read(final InputStream is, final byte[] buf) throws IOException {
        return read(is, buf, 0, buf.length);
    }

    /**
     *
     * @param is
     * @param buf
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static int read(final InputStream is, final byte[] buf, final int off, final int len) throws IOException {
        if ((off < 0) || (off > buf.length) || (len < 0) || ((off + len) > buf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }

        int n = is.read(buf, off, len);

        if (n < 0 || n == len) {
            return n;
        }

        while (n < len) {
            int n1 = is.read(buf, off + n, len - n);

            if (n1 < 0) {
                break;
            }

            n += n1;
        }

        return n;
    }

    /**
     *
     * @param file
     * @param buf
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static int read(final File file, final char[] buf) throws UncheckedIOException {
        return read(file, buf, 0, buf.length);
    }

    /**
     *
     * @param file
     * @param buf
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static int read(final File file, final char[] buf, final Charset charset) throws UncheckedIOException {
        return read(file, buf, 0, buf.length, charset);
    }

    /**
     *
     * @param file
     * @param buf
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static int read(final File file, final char[] buf, final int off, final int len) throws UncheckedIOException {
        return read(file, buf, off, len, Charsets.UTF_8);
    }

    /**
     *
     * @param file
     * @param buf
     * @param off
     * @param len
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static int read(final File file, final char[] buf, final int off, final int len, final Charset charset) throws UncheckedIOException {
        Reader reader = null;

        try {
            reader = new InputStreamReader(new FileInputStream(file), charset == null ? Charsets.UTF_8 : charset);
            return read(reader, buf, off, len);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     *
     * @param reader
     * @param buf
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static int read(final Reader reader, final char[] buf) throws IOException {
        return read(reader, buf, 0, buf.length);
    }

    /**
     *
     * @param reader
     * @param buf
     * @param off the start offset in array <code>b</code> at which the data is written.
     * @param len
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static int read(final Reader reader, final char[] buf, final int off, final int len) throws IOException {
        if ((off < 0) || (off > buf.length) || (len < 0) || ((off + len) > buf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }

        int n = reader.read(buf, off, len);

        if (n < 0 || n == len) {
            return n;
        }

        while (n < len) {
            int n1 = reader.read(buf, off + n, len - n);

            if (n1 < 0) {
                break;
            }

            n += n1;
        }

        return n;
    }

    /**
     *
     * @param file
     * @param obj
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLine(final File file, final Object obj) throws UncheckedIOException {
        Writer writer = null;

        try {
            writer = new FileWriter(file);

            writeLine(writer, obj);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param os
     * @param obj
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLine(final OutputStream os, final Object obj) throws UncheckedIOException {
        writeLine(os, obj, false);
    }

    /**
     *
     * @param os
     * @param obj
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLine(final OutputStream os, final Object obj, final boolean flush) throws UncheckedIOException {
        writeLine(new OutputStreamWriter(os), obj, flush);
    }

    /**
     *
     * @param writer
     * @param obj
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLine(final Writer writer, final Object obj) throws UncheckedIOException {
        writeLine(writer, obj, false);
    }

    /**
     *
     * @param writer
     * @param obj
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLine(final Writer writer, final Object obj, final boolean flush) throws UncheckedIOException {
        try {
            if (obj == null) {
                writer.write(N.NULL_CHAR_ARRAY);
            } else {
                writer.write(N.toString(obj));
            }

            writer.write(IOUtil.LINE_SEPARATOR);

            if (flush) {
                writer.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param file
     * @param lines
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final File file, final Object[] lines) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(file, lines, 0, lines.length);
    }

    /**
     *
     * @param file
     * @param lines
     * @param offset
     * @param count
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final File file, final Object[] lines, final int offset, final int count) throws UncheckedIOException {
        if (count == 0 && N.len(lines) >= offset) {
            return;
        }

        Writer writer = null;

        try {
            writer = new FileWriter(file);

            writeLines(writer, lines, offset, count);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param os
     * @param lines
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final OutputStream os, final Object[] lines) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(os, lines, false);
    }

    /**
     *
     * @param os
     * @param lines
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final OutputStream os, final Object[] lines, final boolean flush) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(os, lines, 0, lines.length, flush);
    }

    /**
     *
     * @param os
     * @param lines
     * @param offset
     * @param count
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final OutputStream os, final Object[] lines, final int offset, final int count) throws UncheckedIOException {
        if (count == 0 && N.len(lines) >= offset) {
            return;
        }

        writeLines(os, lines, offset, count, false);
    }

    /**
     *
     * @param os
     * @param lines
     * @param offset
     * @param count
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final OutputStream os, final Object[] lines, final int offset, final int count, final boolean flush)
            throws UncheckedIOException {
        if (count == 0 && N.len(lines) >= offset) {
            return;
        }

        writeLines(new OutputStreamWriter(os), lines, offset, count, flush);
    }

    /**
     *
     * @param writer
     * @param lines
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final Writer writer, final Object[] lines) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(writer, lines, false);
    }

    /**
     *
     * @param writer
     * @param lines
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final Writer writer, final Object[] lines, final boolean flush) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(writer, lines, 0, lines.length, flush);
    }

    /**
     *
     * @param writer
     * @param lines
     * @param offset
     * @param count
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final Writer writer, final Object[] lines, final int offset, final int count) throws UncheckedIOException {
        if (count == 0 && N.len(lines) >= offset) {
            return;
        }

        writeLines(writer, lines, offset, count, false);
    }

    /**
     *
     * @param writer
     * @param lines
     * @param offset
     * @param count
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final Writer writer, final Object[] lines, final int offset, int count, final boolean flush) throws UncheckedIOException {
        if (count == 0 && N.len(lines) >= offset) {
            return;
        }

        boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
        final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);

        try {
            int lineNum = 0;

            for (Object line : lines) {
                if (lineNum++ >= offset) {
                    if (line == null) {
                        writer.write(N.NULL_CHAR_ARRAY);
                    } else {
                        writer.write(N.toString(line));
                    }

                    writer.write(IOUtil.LINE_SEPARATOR);

                    count--;
                }

                if (count <= 0) {
                    break;
                }
            }

            if (flush || !isBufferedWriter) {
                bw.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    /**
     *
     * @param file
     * @param lines
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final File file, final Collection<?> lines) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(file, lines, 0, lines.size());
    }

    /**
     *
     * @param file
     * @param lines
     * @param offset
     * @param count
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final File file, final Collection<?> lines, final int offset, final int count) throws UncheckedIOException {
        if (count == 0 && N.size(lines) >= offset) {
            return;
        }

        Writer writer = null;

        try {
            writer = new FileWriter(file);

            writeLines(writer, lines, offset, count);

            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param os
     * @param lines
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final OutputStream os, final Collection<?> lines) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(os, lines, false);
    }

    /**
     *
     * @param os
     * @param lines
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final OutputStream os, final Collection<?> lines, final boolean flush) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(os, lines, 0, lines.size(), flush);
    }

    /**
     *
     * @param os
     * @param lines
     * @param offset
     * @param count
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final OutputStream os, final Collection<?> lines, final int offset, final int count) throws UncheckedIOException {
        if (count == 0 && N.size(lines) >= offset) {
            return;
        }

        writeLines(os, lines, offset, count, false);
    }

    /**
     *
     * @param os
     * @param lines
     * @param offset
     * @param count
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final OutputStream os, final Collection<?> lines, final int offset, final int count, final boolean flush)
            throws UncheckedIOException {
        if (count == 0 && N.size(lines) >= offset) {
            return;
        }

        writeLines(new OutputStreamWriter(os), lines, offset, count, flush);
    }

    /**
     *
     * @param writer
     * @param lines
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final Writer writer, final Collection<?> lines) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(writer, lines, false);
    }

    /**
     *
     * @param writer
     * @param lines
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final Writer writer, final Collection<?> lines, final boolean flush) throws UncheckedIOException {
        if (N.isNullOrEmpty(lines)) {
            return;
        }

        writeLines(writer, lines, 0, lines.size(), flush);
    }

    /**
     *
     * @param writer
     * @param lines
     * @param offset
     * @param count
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final Writer writer, final Collection<?> lines, final int offset, final int count) throws UncheckedIOException {
        if (count == 0 && N.size(lines) >= offset) {
            return;
        }

        writeLines(writer, lines, offset, count, false);
    }

    /**
     *
     * @param writer
     * @param lines
     * @param offset
     * @param count
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void writeLines(final Writer writer, final Collection<?> lines, final int offset, int count, final boolean flush)
            throws UncheckedIOException {
        if (count == 0 && N.size(lines) >= offset) {
            return;
        }

        boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
        final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);

        try {
            int lineNum = 0;

            for (Object line : lines) {
                if (lineNum++ >= offset) {
                    if (line == null) {
                        writer.write(N.NULL_CHAR_ARRAY);
                    } else {
                        writer.write(N.toString(line));
                    }

                    writer.write(IOUtil.LINE_SEPARATOR);

                    count--;
                }

                if (count <= 0) {
                    break;
                }
            }

            if (flush || !isBufferedWriter) {
                bw.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedWriter) {
                Objectory.recycle((BufferedWriter) bw);
            }
        }
    }

    /**
     *
     * @param writer
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final boolean b) throws IOException {
        write(writer, b, false);
    }

    /**
     *
     * @param writer
     * @param b
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final boolean b, final boolean flush) throws IOException {
        writer.write(N.stringOf(b));

        if (flush) {
            writer.flush();
        }
    }

    /**
     *
     * @param writer
     * @param c
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final char c) throws IOException {
        write(writer, c, false);
    }

    /**
     *
     * @param writer
     * @param c
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final char c, final boolean flush) throws IOException {
        writer.write(c);

        if (flush) {
            writer.flush();
        }
    }

    /**
     *
     * @param writer
     * @param b
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final byte b) throws IOException {
        write(writer, b, false);
    }

    /**
     *
     * @param writer
     * @param b
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final byte b, final boolean flush) throws IOException {
        writer.write(N.stringOf(b));

        if (flush) {
            writer.flush();
        }
    }

    /**
     *
     * @param writer
     * @param s
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final short s) throws IOException {
        write(writer, s, false);
    }

    /**
     *
     * @param writer
     * @param s
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final short s, final boolean flush) throws IOException {
        writer.write(N.stringOf(s));

        if (flush) {
            writer.flush();
        }
    }

    /**
     *
     * @param writer
     * @param i
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final int i) throws IOException {
        write(writer, i, false);
    }

    /**
     *
     * @param writer
     * @param i
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final int i, final boolean flush) throws IOException {
        writer.write(N.stringOf(i));

        if (flush) {
            writer.flush();
        }
    }

    /**
     *
     * @param writer
     * @param lng
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final long lng) throws IOException {
        write(writer, lng, false);
    }

    /**
     *
     * @param writer
     * @param lng
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final long lng, final boolean flush) throws IOException {
        writer.write(N.stringOf(lng));

        if (flush) {
            writer.flush();
        }
    }

    /**
     *
     * @param writer
     * @param f
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final float f) throws IOException {
        write(writer, f, false);
    }

    /**
     *
     * @param writer
     * @param f
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final float f, final boolean flush) throws IOException {
        writer.write(N.stringOf(f));

        if (flush) {
            writer.flush();
        }
    }

    /**
     *
     * @param writer
     * @param d
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final double d) throws IOException {
        write(writer, d, false);
    }

    /**
     *
     * @param writer
     * @param d
     * @param flush
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final Writer writer, final double d, final boolean flush) throws IOException {
        writer.write(N.stringOf(d));

        if (flush) {
            writer.flush();
        }
    }

    /**
     *
     * @param out
     * @param str
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final File out, final CharSequence str) throws UncheckedIOException {
        write(out, str, Charsets.UTF_8);
    }

    /**
     *
     * @param out
     * @param str
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final File out, final CharSequence str, Charset charset) throws UncheckedIOException {
        charset = charset == null ? Charsets.UTF_8 : charset;

        write(out, chars2Bytes(toCharArray(str), charset));
    }

    /**
     *
     * @param out
     * @param str
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final CharSequence str) throws UncheckedIOException {
        write(out, str, false);
    }

    /**
     *
     * @param out
     * @param str
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final CharSequence str, final Charset charset) throws UncheckedIOException {
        write(out, str, charset, false);
    }

    /**
     *
     * @param out
     * @param str
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final CharSequence str, final boolean flush) throws UncheckedIOException {
        write(out, str, Charsets.UTF_8, flush);
    }

    /**
     *
     * @param out
     * @param str
     * @param charset
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final CharSequence str, Charset charset, final boolean flush) throws UncheckedIOException {
        charset = charset == null ? Charsets.UTF_8 : charset;

        try {
            out.write(chars2Bytes(toCharArray(str), charset));

            if (flush) {
                out.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param out
     * @param str
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final Writer out, final CharSequence str) throws UncheckedIOException {
        write(out, str, false);
    }

    /**
     *
     * @param out
     * @param str
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final Writer out, final CharSequence str, final boolean flush) throws UncheckedIOException {
        write(out, toCharArray(str), flush);
    }

    /**
     *
     * @param out
     * @param chars
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final File out, final char[] chars) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        write(out, chars, 0, chars.length);
    }

    /**
     *
     * @param out
     * @param chars
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final File out, final char[] chars, final Charset charset) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        write(out, chars, 0, chars.length, charset);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final File out, final char[] chars, final int offset, final int len) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        write(out, chars, offset, len, Charsets.UTF_8);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final File out, final char[] chars, final int offset, final int len, final Charset charset) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        write(out, chars2Bytes(chars, offset, len, charset));
    }

    /**
     *
     * @param out
     * @param chars
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final char[] chars) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        write(out, chars, 0, chars.length);
    }

    /**
     *
     * @param out
     * @param chars
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final char[] chars, final Charset charset) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        write(out, chars, 0, chars.length, charset);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final char[] chars, final int offset, final int len) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        write(out, chars, offset, len, Charsets.UTF_8);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final char[] chars, final int offset, final int len, final Charset charset) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        write(out, chars, offset, len, charset, false);
    }

    /**
     *
     * @param out
     * @param chars
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final char[] chars, final boolean flush) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        write(out, chars, 0, chars.length, flush);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final char[] chars, final int offset, final int len, final boolean flush) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        write(out, chars, offset, len, Charsets.UTF_8, flush);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @param charset
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final char[] chars, final int offset, final int len, final Charset charset, final boolean flush)
            throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        write(out, chars2Bytes(chars, offset, len, charset), flush);
    }

    /**
     *
     * @param out
     * @param chars
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final Writer out, final char[] chars) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        write(out, chars, 0, chars.length);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final Writer out, final char[] chars, final int offset, final int len) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        write(out, chars, offset, len, false);
    }

    /**
     *
     * @param out
     * @param chars
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final Writer out, final char[] chars, final boolean flush) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        write(out, chars, 0, chars.length, flush);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final Writer out, final char[] chars, final int offset, final int len, final boolean flush) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        try {
            out.write(chars, offset, len);

            if (flush) {
                out.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param out
     * @param bytes
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final File out, final byte[] bytes) throws UncheckedIOException {
        if (N.isNullOrEmpty(bytes)) {
            return;
        }

        write(out, bytes, 0, bytes.length);
    }

    /**
     *
     * @param out
     * @param bytes
     * @param offset
     * @param len
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final File out, final byte[] bytes, final int offset, final int len) throws UncheckedIOException {
        if (len == 0 && N.len(bytes) >= offset) {
            return;
        }

        OutputStream os = null;

        try {
            if (!out.exists()) {
                out.createNewFile();
            }

            os = new FileOutputStream(out);

            write(os, bytes, offset, len);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(os);
        }
    }

    /**
     *
     * @param out
     * @param bytes
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final byte[] bytes) throws UncheckedIOException {
        if (N.isNullOrEmpty(bytes)) {
            return;
        }

        write(out, bytes, 0, bytes.length);
    }

    /**
     *
     * @param out
     * @param bytes
     * @param offset
     * @param len
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final byte[] bytes, final int offset, final int len) throws UncheckedIOException {
        if (len == 0 && N.len(bytes) >= offset) {
            return;
        }

        write(out, bytes, offset, len, false);
    }

    /**
     *
     * @param out
     * @param bytes
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final byte[] bytes, final boolean flush) throws UncheckedIOException {
        if (N.isNullOrEmpty(bytes)) {
            return;
        }

        write(out, bytes, 0, bytes.length, flush);
    }

    /**
     *
     * @param out
     * @param bytes
     * @param offset
     * @param len
     * @param flush
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void write(final OutputStream out, final byte[] bytes, final int offset, final int len, final boolean flush) throws UncheckedIOException {
        if (len == 0 && N.len(bytes) >= offset) {
            return;
        }

        try {
            out.write(bytes, offset, len);

            if (flush) {
                out.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final File output, final InputStream input) throws UncheckedIOException {
        return write(output, input, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset by byte
     * @param len by byte
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final File output, final InputStream input, final long offset, final long len) throws UncheckedIOException {
        OutputStream os = null;

        try {
            if (!output.exists()) {
                output.createNewFile();
            }

            os = new FileOutputStream(output);

            long result = write(os, input, offset, len);

            os.flush();

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(os);
        }
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final OutputStream output, final InputStream input) throws UncheckedIOException {
        return write(output, input, false);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final OutputStream output, final InputStream input, final long offset, final long len) throws UncheckedIOException {
        return write(output, input, offset, len, false);
    }

    /**
     *
     * @param output
     * @param input
     * @param flush
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final OutputStream output, final InputStream input, final boolean flush) throws UncheckedIOException {
        return write(output, input, 0, Long.MAX_VALUE, flush);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset by byte
     * @param len by byte
     * @param flush
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final OutputStream output, final InputStream input, final long offset, final long len, final boolean flush)
            throws UncheckedIOException {
        final byte[] buf = Objectory.createByteArrayBuffer();

        try {
            if (offset > 0) {
                skipFully(input, offset);
            }

            if (len == 0) {
                return 0;
            }

            final int bufLength = buf.length;
            long totalCount = 0;
            int count = 0;

            while ((totalCount < len) && (EOF != (count = read(input, buf, 0, (int) Math.min(len - totalCount, bufLength))))) {
                output.write(buf, 0, count);

                totalCount += count;
            }

            if (flush) {
                output.flush();
            }

            return totalCount;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final File output, final Reader input) throws UncheckedIOException {
        return write(output, input, Charsets.UTF_8);
    }

    /**
     *
     * @param output
     * @param input
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final File output, final Reader input, final Charset charset) throws UncheckedIOException {
        return write(output, input, 0, Long.MAX_VALUE, charset);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final File output, final Reader input, final long offset, final long len) throws UncheckedIOException {
        return write(output, input, offset, len, Charsets.UTF_8);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset by char
     * @param len by char
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final File output, final Reader input, final long offset, final long len, final Charset charset) throws UncheckedIOException {
        Writer writer = null;

        try {
            writer = new OutputStreamWriter(new FileOutputStream(output), charset == null ? Charsets.UTF_8 : charset);

            long result = write(writer, input, offset, len);

            writer.flush();

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final Writer output, final Reader input) throws UncheckedIOException {
        return write(output, input, false);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final Writer output, final Reader input, final long offset, final long len) throws UncheckedIOException {
        return write(output, input, offset, len, false);
    }

    /**
     *
     * @param output
     * @param input
     * @param flush
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final Writer output, final Reader input, final boolean flush) throws UncheckedIOException {
        return write(output, input, 0, Long.MAX_VALUE, flush);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset by char
     * @param len by char
     * @param flush
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final Writer output, final Reader input, final long offset, final long len, final boolean flush) throws UncheckedIOException {
        final char[] buf = Objectory.createCharArrayBuffer();

        try {
            if (offset > 0) {
                skipFully(input, offset);
            }

            if (len == 0) {
                return 0;
            }

            final int bufLength = buf.length;
            long totalCount = 0;
            int count = 0;

            while ((totalCount < len) && (EOF != (count = read(input, buf, 0, (int) Math.min(len - totalCount, bufLength))))) {
                output.write(buf, 0, count);

                totalCount += count;
            }

            if (flush) {
                output.flush();
            }

            return totalCount;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);
        }
    }

    //    public static OutputStream base64Wrap(OutputStream os) {
    //        return Base64.getEncoder().wrap(os);
    //    }
    //
    //    public static InputStream base64Wrap(InputStream is) {
    //        return Base64.getDecoder().wrap(is);
    //    }

    // ============================= Java 8 only
    //    public static OutputStream base64Wrap(OutputStream os) {
    //        return java.util.Base64.getEncoder().wrap(os);
    //    }
    //
    //    public static InputStream base64Wrap(InputStream is) {
    //        return java.util.Base64.getDecoder().wrap(is);
    //    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final File output, final File input) throws UncheckedIOException {
        return write(output, input, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final File output, final File input, final long offset, final long len) throws UncheckedIOException {
        OutputStream os = null;
        InputStream is = null;

        try {
            os = new FileOutputStream(output);
            is = new FileInputStream(input);

            return write(os, is, offset, len, true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeQuietly(os);
            closeQuietly(is);
        }
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final OutputStream output, final File input) throws UncheckedIOException {
        return write(output, input, false);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final OutputStream output, final File input, final long offset, final long len) throws UncheckedIOException {
        return write(output, input, offset, len, false);
    }

    /**
     *
     * @param output
     * @param input
     * @param flush
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final OutputStream output, final File input, final boolean flush) throws UncheckedIOException {
        return write(output, input, 0, Long.MAX_VALUE, flush);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset by byte
     * @param len by byte
     * @param flush
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final OutputStream output, final File input, final long offset, final long len, final boolean flush) throws UncheckedIOException {
        InputStream is = null;
        try {
            is = new FileInputStream(input);

            return write(output, is, offset, len, flush);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeQuietly(is);
        }
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final Writer output, final File input) throws UncheckedIOException {
        return write(output, input, false);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final Writer output, final File input, final long offset, final long len) throws UncheckedIOException {
        return write(output, input, offset, len, false);
    }

    /**
     *
     * @param output
     * @param input
     * @param flush
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final Writer output, final File input, final boolean flush) throws UncheckedIOException {
        return write(output, input, 0, Long.MAX_VALUE, flush);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset by char
     * @param len by char
     * @param flush
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long write(final Writer output, final File input, final long offset, final long len, final boolean flush) throws UncheckedIOException {
        Reader reader = null;
        try {
            reader = new FileReader(input);

            return write(output, reader, offset, len, flush);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeQuietly(reader);
        }
    }

    /**
     *
     * @param out
     * @param bytes
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void append(final File out, final byte[] bytes) throws UncheckedIOException {
        if (N.isNullOrEmpty(bytes)) {
            return;
        }

        append(out, bytes, 0, bytes.length);
    }

    /**
     *
     * @param out
     * @param bytes
     * @param offset
     * @param len
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void append(final File out, final byte[] bytes, final int offset, final int len) throws UncheckedIOException {
        if (len == 0 && N.len(bytes) >= offset) {
            return;
        }

        OutputStream os = null;

        try {
            if (!out.exists()) {
                out.createNewFile();
            }

            os = new FileOutputStream(out, true);

            write(os, bytes, offset, len);

            os.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(os);
        }
    }

    /**
     *
     * @param out
     * @param chars
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void append(final File out, final char[] chars) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        append(out, chars, 0, chars.length);
    }

    /**
     *
     * @param out
     * @param chars
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void append(final File out, final char[] chars, final Charset charset) throws UncheckedIOException {
        if (N.isNullOrEmpty(chars)) {
            return;
        }

        append(out, chars, 0, chars.length, charset);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void append(final File out, final char[] chars, final int offset, final int len) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        append(out, chars, offset, len, Charsets.UTF_8);
    }

    /**
     *
     * @param out
     * @param chars
     * @param offset
     * @param len
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void append(final File out, final char[] chars, final int offset, final int len, final Charset charset) throws UncheckedIOException {
        if (len == 0 && N.len(chars) >= offset) {
            return;
        }

        append(out, chars2Bytes(chars, offset, len, charset));
    }

    /**
     *
     * @param output
     * @param str
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void append(File output, CharSequence str) throws UncheckedIOException {
        append(output, str, Charsets.UTF_8);
    }

    /**
     *
     * @param output
     * @param str
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void append(File output, CharSequence str, Charset charset) throws UncheckedIOException {
        final char[] chs = toCharArray(str);

        append(output, chs, 0, chs.length, charset);
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long append(final File output, final InputStream input) throws UncheckedIOException {
        return append(output, input, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset by byte
     * @param len by byte
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long append(final File output, final InputStream input, final long offset, final long len) throws UncheckedIOException {
        OutputStream os = null;

        try {
            if (!output.exists()) {
                output.createNewFile();
            }

            os = new FileOutputStream(output, true);

            long result = write(os, input, offset, len);

            os.flush();

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(os);
        }
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long append(final File output, final Reader input) throws UncheckedIOException {
        return append(output, input, Charsets.UTF_8);
    }

    /**
     *
     * @param output
     * @param input
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long append(final File output, final Reader input, final Charset charset) throws UncheckedIOException {
        return append(output, input, 0, Long.MAX_VALUE, charset);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long append(final File output, final Reader input, final long offset, final long len) throws UncheckedIOException {
        return append(output, input, offset, len, Charsets.UTF_8);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset by char
     * @param len by char
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long append(final File output, final Reader input, final long offset, final long len, final Charset charset) throws UncheckedIOException {
        Writer writer = null;

        try {
            writer = new OutputStreamWriter(new FileOutputStream(output, true), charset == null ? Charsets.UTF_8 : charset);

            long result = write(writer, input, offset, len);

            writer.flush();

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(writer);
        }
    }

    /**
     *
     * @param output
     * @param input
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long append(final File output, final File input) throws UncheckedIOException {
        return append(output, input, 0, Long.MAX_VALUE);
    }

    /**
     *
     * @param output
     * @param input
     * @param offset
     * @param len
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long append(final File output, final File input, final long offset, final long len) throws UncheckedIOException {
        OutputStream os = null;
        InputStream is = null;

        try {
            os = new FileOutputStream(output, true);
            is = new FileInputStream(input);

            return write(os, is, offset, len, true);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeQuietly(os);
            closeQuietly(is);
        }
    }

    /**
     *
     * @param output
     * @param str
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void appendLine(File output, CharSequence str) throws UncheckedIOException {
        appendLine(output, str, Charsets.UTF_8);
    }

    /**
     *
     * @param output
     * @param str
     * @param charset
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void appendLine(File output, CharSequence str, Charset charset) throws UncheckedIOException {
        final char[] chs = toCharArray(str + IOUtil.LINE_SEPARATOR);

        append(output, chs, 0, chs.length, charset);
    }

    /**
     * Return the count of skipped bytes.
     *
     * @param input
     * @param toSkip
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long skip(final InputStream input, final long toSkip) throws UncheckedIOException {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static long skip(final Reader input, final long toSkip) throws UncheckedIOException {
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     *
     * @param input
     * @param toSkip
     * @throws UncheckedIOException if the remaining length of the specified <code>input</code> is less than the specified <code>toSkip</code>
     */
    public static void skipFully(final InputStream input, final long toSkip) throws UncheckedIOException {
        if (toSkip < 0) {
            throw new IllegalArgumentException("Bytes to skip must not be negative: " + toSkip);
        }

        long skipped = skip(input, toSkip);

        if (skipped != toSkip) {
            throw new UncheckedIOException(new IOException("Bytes to skip: " + toSkip + " actual: " + skipped));
        }
    }

    /**
     *
     * @param input
     * @param toSkip
     * @throws UncheckedIOException if the remaining length of the specified <code>input</code> is less than the specified <code>toSkip</code>
     */
    public static void skipFully(final Reader input, final long toSkip) throws UncheckedIOException {
        long skipped = skip(input, toSkip);

        if (skipped != toSkip) {
            throw new RuntimeException(new IOException("Chars to skip: " + toSkip + " actual: " + skipped));
        }
    }

    /**
     * Note: copied from Google Guava under Apache License v2.
     *
     * @param file
     * @return
     */
    public static MappedByteBuffer map(File file) {
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
     * <p>This only works for files <= {@link Integer#MAX_VALUE} bytes.
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file}
     * @return a buffer reflecting {@code file}
     * @throws UncheckedIOException the unchecked IO exception
     * @see FileChannel#map(MapMode, long, long)
     * @since 2.0
     */
    public static MappedByteBuffer map(File file, MapMode mode) throws UncheckedIOException {
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
     * <p>This only works for files <= {@link Integer#MAX_VALUE} bytes.
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file}
     * @param offset
     * @param len
     * @return a buffer reflecting {@code file}
     * @throws UncheckedIOException the unchecked IO exception
     * @see FileChannel#map(MapMode, long, long)
     * @since 2.0
     */
    public static MappedByteBuffer map(File file, MapMode mode, long offset, long len) throws UncheckedIOException {
        N.checkArgNotNull(file);
        N.checkArgNotNull(mode);

        RandomAccessFile raf = null;

        try {
            raf = new RandomAccessFile(file, mode == MapMode.READ_ONLY ? "r" : "rw");
            return raf.getChannel().map(mode, offset, len);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
     * <li>fold out ./
     * <li>fold out ../ when possible
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
        if (N.isNullOrEmpty(pathname)) {
            return ".";
        }

        pathname = pathname.replace('\\', '/');

        // split the path apart
        String[] components = pathSplitter.splitToArray(pathname);
        List<String> path = new ArrayList<>();

        // resolve ., .., and //
        for (String component : components) {
            if (component.length() == 0 || component.equals(".")) {
                continue;
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
        String result = StringUtil.join(path, '/');

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
     * @since 11.0
     */
    public static String getFileExtension(String fullName) {
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
     * @since 14.0
     */
    public static String getNameWithoutExtension(String file) {
        N.checkArgNotNull(file);

        String fileName = new File(file).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
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
        try {
            return new java.io.BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
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
        try {
            return new java.io.BufferedReader(new InputStreamReader(new FileInputStream(file), charset == null ? Charsets.UTF_8 : charset));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
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
            return Files.newBufferedReader(path, Charsets.UTF_8);
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
        return new java.io.BufferedReader(new InputStreamReader(is));
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
        return new java.io.BufferedReader(new InputStreamReader(is, charset == null ? Charsets.UTF_8 : charset));
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
        try {
            return new java.io.BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        try {
            return new java.io.BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset == null ? Charsets.UTF_8 : charset));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * New buffered writer.
     *
     * @param os
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedWriter newBufferedWriter(OutputStream os) throws UncheckedIOException {
        return new java.io.BufferedWriter(new OutputStreamWriter(os));
    }

    /**
     * New buffered writer.
     *
     * @param os
     * @param charset
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static java.io.BufferedWriter newBufferedWriter(OutputStream os, Charset charset) throws UncheckedIOException {
        return new java.io.BufferedWriter(new OutputStreamWriter(os, charset == null ? Charsets.UTF_8 : charset));
    }

    /**
     * New LZ 4 block input stream.
     *
     * @param is
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static LZ4BlockInputStream newLZ4BlockInputStream(final InputStream is) throws UncheckedIOException {
        try {
            return new LZ4BlockInputStream(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * New LZ 4 block output stream.
     *
     * @param os
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os) throws UncheckedIOException {
        try {
            return new LZ4BlockOutputStream(os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new input stream with the specified buffer size.
     *
     * @param os
     * @param blockSize
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os, final int blockSize) throws UncheckedIOException {
        try {
            return new LZ4BlockOutputStream(os, blockSize);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os) throws UncheckedIOException {
        try {
            return new SnappyOutputStream(os);
        } catch (IOException e) {
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
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os, final int bufferSize) throws UncheckedIOException {
        try {
            return new SnappyOutputStream(os, bufferSize);
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
     *
     * @param closeable
     */
    public static void close(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                throw N.toRuntimeException(e);
            }
        }
    }

    /**
     *
     * @param a
     */
    @SafeVarargs
    public static void closeAll(final AutoCloseable... a) {
        if (N.isNullOrEmpty(a)) {
            return;
        }

        closeAll(Arrays.asList(a));
    }

    /**
     *
     * @param c
     */
    public static void closeAll(final Collection<? extends AutoCloseable> c) {
        if (N.isNullOrEmpty(c)) {
            return;
        }

        Throwable ex = null;

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
            throw N.toRuntimeException(ex);
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
        if (N.isNullOrEmpty(a)) {
            return;
        }

        closeAllQuietly(Arrays.asList(a));
    }

    /**
     * Close all quietly.
     *
     * @param c
     */
    public static void closeAllQuietly(final Collection<? extends AutoCloseable> c) {
        if (N.isNullOrEmpty(c)) {
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
     */
    public static void copy(final File srcFile, final File destDir) {
        copy(srcFile, destDir, true);
    }

    /**
     *
     * @param srcFile
     * @param destDir
     * @param preserveFileDate
     */
    public static void copy(final File srcFile, final File destDir, final boolean preserveFileDate) {
        copy(srcFile, destDir, preserveFileDate, Fn.BiPredicates.alwaysTrue());
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
    public static <E extends Exception> void copy(File srcFile, File destDir, final boolean preserveFileDate,
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

        if (destDir.canWrite() == false) {
            throw new UncheckedIOException(new IOException("Destination '" + destDir + "' cannot be written to"));
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

        if (N.isNullOrEmpty(subFiles)) {
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
            destDir.setLastModified(srcDir.lastModified());
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
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
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
            destFile.setLastModified(srcFile.lastModified());
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
     * @param out
     * @return
     * @throws UncheckedIOException the unchecked IO exception
     * @see Files#copy(Path, OutputStream)
     */
    public static long copy(Path source, OutputStream out) throws UncheckedIOException {
        try {
            return Files.copy(source, out);
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

            write(destination, is);
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

            write(destination, is);
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
     */
    public static boolean deleteIfExists(final File file) {
        if ((file == null) || !file.exists()) {
            return false;
        }

        return file.delete();
    }

    /**
     * Delete the specified file and all its sub files/directories if it's a directory.
     *
     * @param file
     * @return true if the file is deleted successfully, otherwise false if the file is null or doesn't exist, or can't be deleted.
     */
    public static boolean deleteAllIfExists(final File file) {
        if ((file == null) || !file.exists()) {
            return false;
        }

        if (file.isDirectory()) {
            final File[] files = file.listFiles();

            if (N.notNullOrEmpty(files)) {
                for (File subFile : files) {
                    if (subFile == null) {
                        continue;
                    }

                    if (subFile.isFile()) {
                        if (subFile.delete() == false) {
                            return false;
                        }
                    } else {
                        if (deleteAllIfExists(subFile) == false) {
                            return false;
                        }
                    }
                }
            }
        }

        return file.delete();
    }

    /**
     *
     * @param dir
     * @return true, if successful
     */
    public static boolean deleteFiles(final File dir) {
        return deleteFiles(dir, BiPredicates.alwaysTrue());
    }

    /**
     * Delete the specifield <code>dir</code> if it's a file or its sub files/directories if it's a directory with the specified filter.
     *
     * @param <E>
     * @param dir
     * @param filter
     * @return true, if successful
     * @throws E the e
     */
    public static <E extends Exception> boolean deleteFiles(final File dir, Throwables.BiPredicate<? super File, ? super File, E> filter) throws E {
        if ((dir == null) || !dir.exists()) {
            return false;
        }

        if (dir.isDirectory()) {
            final File[] files = dir.listFiles();

            if (N.isNullOrEmpty(files)) {
                return true;
            }

            for (File subFile : files) {
                if (subFile == null) {
                    continue;
                }

                if (filter == null || filter.test(dir, subFile)) {
                    if (subFile.isFile()) {
                        if (subFile.delete() == false) {
                            return false;
                        }
                    } else {
                        if (deleteAllIfExists(subFile) == false) {
                            return false;
                        }
                    }
                } else {
                    if (subFile.isDirectory()) {
                        if (deleteFiles(subFile, filter) == false) {
                            return false;
                        }
                    }
                }
            }
        } else {
            if (filter == null || filter.test(dir.getParentFile(), dir)) {
                return dir.delete();
            }
        }

        return true;
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
     * @param sourceFile
     * @param targetFile
     * @throws UncheckedIOException the unchecked IO exception
     */
    public static void zip(final File sourceFile, final File targetFile) throws UncheckedIOException {
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(new FileOutputStream(targetFile));
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

        try {
            zos = new ZipOutputStream(new FileOutputStream(targetFile));

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
     * @throws FileNotFoundException the file not found exception
     */
    private static void zipFile(final File sourceFile, final ZipOutputStream zos, final File targetFile) throws IOException, FileNotFoundException {
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
     * @throws FileNotFoundException the file not found exception
     */
    private static void zipFile(final File file, final File sourceDir, final ZipOutputStream zos, final File targetFile)
            throws IOException, FileNotFoundException {
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

        InputStream is = new FileInputStream(file);

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

        try {
            zip = new ZipFile(srcZipFile);

            Enumeration<? extends ZipEntry> entryEnum = zip.entries();

            while (entryEnum.hasMoreElements()) {
                ze = entryEnum.nextElement();

                if (ze.isDirectory()) {
                    continue;
                }

                os = new FileOutputStream(getAbsolutePath(targetDir, ze.getName()));

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
            input = new FileInputStream(file);

            for (int i = 0; i < numOfParts; i++) {
                String subFileNmae = destDir.getAbsolutePath() + IOUtil.FILE_SEPARATOR + fileName + "_" + StringUtil.padStart(N.stringOf(fileSerNum++), 4, '0');
                output = new FileOutputStream(new File(subFileNmae));
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
    static void splitByLine(final File file, final int numOfParts, final File destDir) throws UncheckedIOException {
        final long lineNumOfPart = estimateLineCount(file, 10000) / numOfParts;

        int index = file.getName().lastIndexOf('.');
        String prefix = file.getName().substring(0, index);
        String postfix = (index > 0) ? file.getName().substring(index) : "";

        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        BufferedReader br = null;
        BufferedWriter bw = null;
        int fileSerNum = 1;

        try {
            is = openFile(outputZipFile, file);

            br = Objectory.createBufferedReader(is);

            String subFileNmae = destDir.getAbsolutePath() + IOUtil.FILE_SEPARATOR + prefix + "_" + StringUtil.padStart(N.stringOf(fileSerNum++), 4, '0')
                    + postfix;
            bw = Objectory.createBufferedWriter(new FileWriter(new File(subFileNmae)));

            int lineCounter = 0;
            String line = null;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.write(IOUtil.LINE_SEPARATOR);
                lineCounter++;

                if ((lineCounter % lineNumOfPart) == 0) {
                    if (bw != null) {
                        close(bw);
                        Objectory.recycle(bw);
                        bw = null;
                    }

                    subFileNmae = destDir.getAbsolutePath() + IOUtil.FILE_SEPARATOR + prefix + "_" + StringUtil.padStart(N.stringOf(fileSerNum++), 4, '0')
                            + postfix;
                    bw = Objectory.createBufferedWriter(new FileWriter(new File(subFileNmae)));
                }
            }

            if (bw != null) {
                close(bw);
                Objectory.recycle(bw);
                bw = null;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (bw != null) {
                close(bw);
                Objectory.recycle(bw);
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
            is = openFile(outputZipFile, file);

            br = Objectory.createBufferedReader(is);

            int cnt = 0;
            String line = null;
            long bytes = 0;
            while (cnt < byReadingLineNum && (line = br.readLine()) != null) {
                bytes += line.getBytes().length;

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
            output = new FileOutputStream(destFile);

            InputStream input = null;
            for (File file : sourceFiles) {
                try {
                    input = new FileInputStream(file);

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
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static String getAbsolutePath(final File parentDir, String relativeFilePath) throws IOException {
        String newRelativePath = "";

        for (int i = 0; i < relativeFilePath.length(); i++) {
            char c = relativeFilePath.charAt(i);

            if ((c == '\\') || (c == '/')) {
                newRelativePath += File.separator;
            } else {
                newRelativePath += c;
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

        if (N.isNullOrEmpty(subFiles)) {
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

        if (N.isNullOrEmpty(subFiles)) {
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
        if (url.getProtocol().equals("file") == false) {
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
            final StringBuffer buffer = new StringBuffer();
            final ByteBuffer bytes = ByteBuffer.allocate(n);
            for (int i = 0; i < n;) {
                if (url.charAt(i) == '%') {
                    try {
                        do {
                            final byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                            bytes.put(octet);
                            i += 3;
                        } while (i < n && url.charAt(i) == '%');
                        continue;
                    } catch (final RuntimeException e) {
                        // malformed percent-encoded octet, fall through and
                        // append characters literally
                    } finally {
                        if (bytes.position() > 0) {
                            bytes.flip();
                            buffer.append(Charsets.UTF_8.decode(bytes).toString());
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
        if (N.isNullOrEmpty(urls)) {
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
        if (N.isNullOrEmpty(urls)) {
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
        if (N.isNullOrEmpty(files)) {
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
     */
    public static List<URL> toURLs(final Collection<File> files) {
        if (N.isNullOrEmpty(files)) {
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
     * @param file the File to touch
     * @return true if the file exists and last modified time is updated successfully.
     */
    public static boolean touch(final File file) {
        return file.exists() && file.setLastModified(System.currentTimeMillis());
    }

    /**
     *
     * @param <E>
     * @param file
     * @param lineParser
     * @throws E the e
     */
    public static <E extends Exception> void parse(final File file, final Throwables.Consumer<String, E> lineParser) throws E {
        parse(file, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param file
     * @param lineParser
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File file, final Throwables.Consumer<String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws E, E2 {
        parse(file, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param file
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final File file, final long lineOffset, final long count, final Throwables.Consumer<String, E> lineParser)
            throws UncheckedIOException, E {
        parse(file, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param file
     * @param lineOffset
     * @param count
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File file, final long lineOffset, final long count,
            final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(file, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param file
     * @param lineOffset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final File file, final long lineOffset, final long count, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
        parse(file, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified files/directory line by line.
     *
     * @param <E>
     * @param <E2>
     * @param file parse all the sub files recursively if the element is a directory.
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
    public static <E extends Exception, E2 extends Exception> void parse(final File file, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(file.isDirectory() ? listFiles(file, true, true) : Array.asList(file), lineOffset, count, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param files
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final List<File> files, final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
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
    public static <E extends Exception, E2 extends Exception> void parse(final List<File> files, final Throwables.Consumer<String, E> lineParser,
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
    public static <E extends Exception> void parse(final List<File> files, final long lineOffset, final long count, final Throwables.Consumer<String, E> lineParser)
            throws UncheckedIOException, E {
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
    public static <E extends Exception, E2 extends Exception> void parse(final List<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
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
    public static <E extends Exception> void parse(final List<File> files, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
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
    public static <E extends Exception, E2 extends Exception> void parse(final List<File> files, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        if (N.isNullOrEmpty(files)) {
            return;
        }

        final List<Reader> readers = new ArrayList<>(files.size());

        try {
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

            Iterables.parse(iterators, lineOffset, count, 0, processThreadNum, queueSize, lineParser, onComplete);
        } finally {
            for (Reader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    /**
     *
     * @param <E>
     * @param file
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final File file, final int readThreadNum, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
        parse(file, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param file
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final File file, final int readThreadNum, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(file, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param file
     * @param lineOffset
     * @param count
     * @param readThreadNum
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final File file, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
        parse(file, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified files/directory line by line.
     *
     * @param <E>
     * @param <E2>
     * @param file parse all the sub files recursively if the element is a directory.
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
    public static <E extends Exception, E2 extends Exception> void parse(final File file, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        parse(file.isDirectory() ? listFiles(file, true, true) : Array.asList(file), lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser,
                onComplete);
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
    public static <E extends Exception> void parse(final List<File> files, final int readThreadNum, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
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
    public static <E extends Exception, E2 extends Exception> void parse(final List<File> files, final int readThreadNum, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
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
    public static <E extends Exception> void parse(final List<File> files, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
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
    public static <E extends Exception, E2 extends Exception> void parse(final List<File> files, final long lineOffset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        if (N.isNullOrEmpty(files)) {
            return;
        }

        final List<Reader> readers = new ArrayList<>(files.size());

        try {
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

            Iterables.parse(iterators, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineParser, onComplete);
        } finally {
            for (Reader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    /**
     *
     * @param <E>
     * @param is
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final InputStream is, final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
        parse(is, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param is
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final InputStream is, final Throwables.Consumer<String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(is, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param is
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final InputStream is, final long lineOffset, final long count, final Throwables.Consumer<String, E> lineParser)
            throws UncheckedIOException, E {
        parse(is, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param is
     * @param lineOffset
     * @param count
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final InputStream is, final long lineOffset, final long count,
            final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(is, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param is
     * @param lineOffset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final InputStream is, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
        parse(is, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param <E2>
     * @param is
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
    public static <E extends Exception, E2 extends Exception> void parse(final InputStream is, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        final BufferedReader br = Objectory.createBufferedReader(is);

        try {
            parse(br, lineOffset, count, processThreadNum, queueSize, lineParser, onComplete);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     *
     * @param <E>
     * @param reader
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Reader reader, final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
        parse(reader, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param reader
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Reader reader, final Throwables.Consumer<String, E> lineParser,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(reader, 0, Long.MAX_VALUE, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param reader
     * @param lineOffset
     * @param count
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Reader reader, final long lineOffset, final long count, final Throwables.Consumer<String, E> lineParser)
            throws UncheckedIOException, E {
        parse(reader, lineOffset, count, lineParser, Fn.emptyAction());
    }

    /**
     *
     * @param <E>
     * @param <E2>
     * @param reader
     * @param lineOffset
     * @param count
     * @param lineParser
     * @param onComplete
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     * @throws E2 the e2
     */
    public static <E extends Exception, E2 extends Exception> void parse(final Reader reader, final long lineOffset, final long count,
            final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        parse(reader, lineOffset, count, 0, 0, lineParser, onComplete);
    }

    /**
     *
     * @param <E>
     * @param reader
     * @param lineOffset
     * @param count
     * @param processThreadNum
     * @param queueSize
     * @param lineParser
     * @throws UncheckedIOException the unchecked IO exception
     * @throws E the e
     */
    public static <E extends Exception> void parse(final Reader reader, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<String, E> lineParser) throws UncheckedIOException, E {
        parse(reader, lineOffset, count, processThreadNum, queueSize, lineParser, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>
     * @param <E2>
     * @param reader
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
    public static <E extends Exception, E2 extends Exception> void parse(final Reader reader, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<String, E> lineParser, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        Iterables.parse(new LineIterator(reader), lineOffset, count, processThreadNum, queueSize, lineParser, onComplete);
    }

    /**
     *
     * @param outputZipFile
     * @param file
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static InputStream openFile(final Holder<ZipFile> outputZipFile, final File file) throws IOException {
        InputStream is = null;

        if (file.getName().endsWith(GZ)) {
            is = new GZIPInputStream(new FileInputStream(file));
        } else if (file.getName().endsWith(ZIP)) {
            ZipFile zf = new ZipFile(file);

            ZipEntry ze = zf.entries().nextElement();
            is = zf.getInputStream(ze);
            outputZipFile.setValue(zf);
        } else {
            is = new FileInputStream(file);
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
        return str == null ? N.NULL_CHAR_ARRAY : InternalUtil.getCharsForReadOnly(str instanceof String ? (String) str : str.toString());
    }
}
