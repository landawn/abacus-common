/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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
import java.math.BigInteger;
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
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn.BiPredicates;
import com.landawn.abacus.util.stream.Stream;

/**
 * A comprehensive utility class providing high-performance I/O operations, file manipulation, and stream processing
 * capabilities for Java applications. This class serves as a central hub for all input/output operations in the
 * Abacus framework, offering optimized implementations for file handling, stream operations, compression,
 * and directory management with extensive null-safety and error handling.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>High-Performance I/O:</b> Optimized stream operations with efficient buffer management</li>
 *   <li><b>Comprehensive File Operations:</b> Complete file and directory manipulation capabilities</li>
 *   <li><b>Stream Processing:</b> Advanced stream utilities with automatic resource management</li>
 *   <li><b>Compression Support:</b> Built-in support for ZIP, GZIP, Snappy, and Brotli compression</li>
 *   <li><b>NIO Integration:</b> Modern NIO.2 operations with Path and Channel support</li>
 *   <li><b>Charset Handling:</b> Intelligent charset detection and conversion utilities</li>
 *   <li><b>Memory Mapping:</b> Support for memory-mapped files for large file operations</li>
 *   <li><b>Parallel Processing:</b> Multi-threaded file processing with configurable thread pools</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Resource Management:</b> Automatic resource cleanup with try-with-resources patterns</li>
 *   <li><b>Error Tolerance:</b> Graceful handling of common I/O scenarios without throwing exceptions</li>
 *   <li><b>Performance First:</b> Optimized algorithms with minimal object allocation</li>
 *   <li><b>Null Safety:</b> Comprehensive null checking and defensive programming</li>
 *   <li><b>Cross-Platform:</b> Platform-independent file operations and path handling</li>
 *   <li><b>Memory Efficient:</b> Streaming operations for large files to minimize memory usage</li>
 * </ul>
 *
 * <p><b>Parameter Conventions:</b>
 * <ul>
 *   <li><b>Offset Parameters:</b> Uses {@code offset/count/len} instead of {@code fromIndex/toIndex}</li>
 *   <li><b>Charset Handling:</b> Defaults to {@code Charsets.DEFAULT} for consistent encoding</li>
 *   <li><b>Buffer Sizes:</b> Intelligent default buffer sizes with customization options</li>
 *   <li><b>Exception Handling:</b> {@code UncheckedIOException} wrapping for cleaner API usage</li>
 * </ul>
 *
 * <p><b>Core Operation Categories:</b>
 * <ul>
 *   <li><b>Stream Creation:</b> Factory methods for various stream types with automatic buffering</li>
 *   <li><b>File Operations:</b> Copy, move, delete, create operations with advanced options</li>
 *   <li><b>Directory Management:</b> Recursive operations, listing, traversal, and cleanup</li>
 *   <li><b>Compression/Decompression:</b> ZIP, GZIP, Snappy, Brotli format support</li>
 *   <li><b>Content Processing:</b> Line-by-line processing with parallel execution support</li>
 *   <li><b>File Splitting/Merging:</b> Large file handling with size-based and count-based splitting</li>
 *   <li><b>Content Comparison:</b> Byte-level and content-aware file comparison utilities</li>
 *   <li><b>URL/File Conversion:</b> Bidirectional conversion between URLs and File objects</li>
 * </ul>
 *
 * <p><b>Compression Formats Supported:</b>
 * <ul>
 *   <li><b>ZIP:</b> Standard ZIP compression with custom charset support</li>
 *   <li><b>GZIP:</b> GNU ZIP compression with configurable buffer sizes</li>
 *   <li><b>Snappy:</b> High-speed compression optimized for performance</li>
 *   <li><b>Brotli:</b> Modern compression algorithm with excellent compression ratios</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic file operations
 * IOUtil.copyFile(sourceFile, targetFile);
 * IOUtil.moveFile(sourceFile, targetDirectory);
 * boolean success = IOUtil.deleteIfExists(file);
 *
 * // Stream operations with automatic resource management
 * try (InputStream is = IOUtil.newFileInputStream(file);
 *      OutputStream os = IOUtil.newFileOutputStream(targetFile)) {
 *     IOUtil.copy(is, os);
 * }
 *
 * // Compression operations
 * IOUtil.zip(sourceFiles, targetZipFile);
 * IOUtil.unzip(zipFile, extractDirectory);
 *
 * // Directory operations
 * List<File> files = IOUtil.listFiles(directory, true, false);
 * IOUtil.cleanDirectory(tempDirectory);
 *
 * // Large file processing
 * IOUtil.forLines(largeFile, line -> {
 *     // Process each line efficiently
 *     processLine(line);
 * });
 *
 * // File splitting for large files
 * IOUtil.splitBySize(largeFile, 1024 * 1024); // 1MB parts
 * IOUtil.split(file, 10); // Split into 10 equal parts
 *
 * // Content comparison
 * boolean identical = IOUtil.contentEquals(file1, file2);
 * boolean sameIgnoreEOL = IOUtil.contentEqualsIgnoreEOL(file1, file2, "UTF-8");
 * }</pre>
 *
 * <p><b>Advanced Stream Operations:</b>
 * <pre>{@code
 * // Memory-mapped file operations for large files
 * try (FileChannel channel = IOUtil.newFileChannel(file, "r")) {
 *     MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, file.length());
 *     // Process mapped buffer efficiently
 * }
 *
 * // Parallel line processing with custom thread pool
 * IOUtil.forLines(files, 0, Long.MAX_VALUE, 4, 8, 1000,
 *     line -> processLine(line),
 *     ex -> handleException(ex));
 *
 * // Buffered stream creation with optimal sizes
 * try (BufferedReader reader = IOUtil.newBufferedReader(file, "UTF-8");
 *      BufferedWriter writer = IOUtil.newBufferedWriter(outputFile, "UTF-8")) {
 *     String line;
 *     while ((line = reader.readLine()) != null) {
 *         writer.write(processLine(line));
 *         writer.newLine();
 *     }
 * }
 * }</pre>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>File Copy:</b> O(n) with optimized buffer sizes and NIO channels</li>
 *   <li><b>Directory Traversal:</b> O(n) with efficient file system walking</li>
 *   <li><b>Line Processing:</b> O(n) with streaming approach, memory usage O(1)</li>
 *   <li><b>Compression:</b> Algorithm-dependent, optimized for speed vs. ratio trade-offs</li>
 *   <li><b>File Splitting:</b> O(n) with minimal memory overhead</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Design:</b> All utility methods are stateless and thread-safe</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 *   <li><b>Resource Pools:</b> Internal object pools are thread-safe and lock-free where possible</li>
 *   <li><b>Parallel Operations:</b> Built-in support for parallel processing with configurable thread pools</li>
 * </ul>
 *
 * <p><b>Error Handling Strategy:</b>
 * <ul>
 *   <li><b>UncheckedIOException:</b> Wraps checked {@code IOException} for cleaner API usage</li>
 *   <li><b>Graceful Degradation:</b> Operations continue where possible despite partial failures</li>
 *   <li><b>Resource Cleanup:</b> Automatic cleanup even in error scenarios</li>
 *   <li><b>Detailed Logging:</b> Comprehensive logging for debugging and monitoring</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li><b>Streaming Operations:</b> Process large files without loading into memory</li>
 *   <li><b>Buffer Optimization:</b> Intelligent buffer sizing based on operation type</li>
 *   <li><b>Memory Mapping:</b> Use OS-level memory mapping for very large files</li>
 *   <li><b>Object Pooling:</b> Reuse of expensive objects like channels and buffers</li>
 * </ul>
 *
 * <p><b>Platform Compatibility:</b>
 * <ul>
 *   <li><b>Cross-Platform Paths:</b> Handles platform-specific path separators automatically</li>
 *   <li><b>File System Features:</b> Adapts to file system capabilities (symlinks, permissions)</li>
 *   <li><b>Charset Handling:</b> Robust charset detection and conversion across platforms</li>
 *   <li><b>NIO.2 Integration:</b> Modern file system operations with fallback support</li>
 * </ul>
 *
 * <p><b>Integration with Java NIO:</b>
 * <ul>
 *   <li><b>Path Support:</b> Seamless integration with {@code java.nio.file.Path}</li>
 *   <li><b>Channel Operations:</b> Direct support for NIO channels for high-performance I/O</li>
 *   <li><b>File Attributes:</b> Advanced file attribute handling and manipulation</li>
 *   <li><b>Watch Service:</b> Integration points for file system monitoring</li>
 * </ul>
 *
 * <p><b>Compression Performance:</b>
 * <ul>
 *   <li><b>Snappy:</b> Fastest compression, moderate compression ratio</li>
 *   <li><b>GZIP:</b> Good balance of speed and compression ratio</li>
 *   <li><b>Brotli:</b> Excellent compression ratio, higher CPU usage</li>
 *   <li><b>ZIP:</b> Standard format with good compression and wide compatibility</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use try-with-resources for automatic stream closure</li>
 *   <li>Specify explicit charsets to avoid platform dependencies</li>
 *   <li>Use streaming operations for large files to minimize memory usage</li>
 *   <li>Leverage parallel processing for CPU-intensive file operations</li>
 *   <li>Use appropriate compression format based on speed vs. size requirements</li>
 *   <li>Validate file existence and permissions before operations</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Loading entire large files into memory unnecessarily</li>
 *   <li>Not properly closing streams and channels</li>
 *   <li>Using blocking I/O for high-concurrency applications</li>
 *   <li>Ignoring charset specifications leading to encoding issues</li>
 *   <li>Not handling interruption in long-running operations</li>
 * </ul>
 *
 * <p><b>Extension Points:</b>
 * <ul>
 *   <li><b>Custom Stream Types:</b> Integration with custom InputStream/OutputStream implementations</li>
 *   <li><b>Filter Functions:</b> Custom predicates for file filtering and selection</li>
 *   <li><b>Progress Callbacks:</b> Monitoring long-running operations</li>
 *   <li><b>Custom Charsets:</b> Support for application-specific character encodings</li>
 * </ul>
 *
 * <p><b>Related Utility Classes:</b>
 * <ul>
 *   <li><b>{@link java.nio.file.Files}:</b> Standard Java NIO.2 file operations</li>
 *   <li><b>{@link com.landawn.abacus.guava.Files}:</b> Guava-style file utilities</li>
 *   <li><b>{@link com.landawn.abacus.util.Strings}:</b> String manipulation utilities</li>
 *   <li><b>{@link com.landawn.abacus.util.Iterators}:</b> Iterator and collection utilities</li>
 *   <li><b>{@link com.landawn.abacus.util.FilenameUtil}:</b> Filename and path manipulation</li>
 *   <li><b>{@link com.landawn.abacus.util.stream.Stream}:</b> Enhanced stream processing</li>
 * </ul>
 *
 * <p><b>Example: Complete File Processing Pipeline</b>
 * <pre>{@code
 * // Process large log files with parallel processing
 * File logDirectory = new File("/var/logs");
 * File outputDirectory = new File("/processed");
 *
 * // List all log files recursively
 * List<File> logFiles = IOUtil.listFiles(logDirectory, true, false)
 *     .stream()
 *     .filter(f -> f.getName().endsWith(".log"))
 *     .collect(Collectors.toList());
 *
 * // Process each file in parallel
 * logFiles.parallelStream().forEach(logFile -> {
 *     File outputFile = new File(outputDirectory, logFile.getName() + ".processed");
 *     
 *     try {
 *         IOUtil.forLines(logFile, 0, Long.MAX_VALUE, 4,
 *             line -> {
 *                 // Process each line
 *                 String processed = processLogLine(line);
 *                 if (processed != null) {
 *                     synchronized (outputFile) {
 *                         IOUtil.write(outputFile, processed + "\n", true);
 *                     }
 *                 }
 *             },
 *             ex -> logger.error("Error processing line", ex));
 *     } catch (Exception e) {
 *         logger.error("Error processing file: " + logFile, e);
 *     }
 * });
 *
 * // Compress processed files
 * IOUtil.zip(IOUtil.listFiles(outputDirectory), new File("processed_logs.zip"));
 * }</pre>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other
 * open source projects under the Apache License 2.0. Methods from these libraries may have been
 * modified for consistency, performance optimization, and null-safety enhancement.
 *
 * @see java.nio.file.Files
 * @see com.landawn.abacus.guava.Files
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.FilenameUtil
 * @see com.landawn.abacus.util.stream.Stream
 * @see java.io.InputStream
 * @see java.io.OutputStream
 * @see java.nio.channels.FileChannel
 * @see java.nio.file.Path
 * @see java.util.zip.ZipInputStream
 * @see java.util.zip.GZIPInputStream
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

    /**
     * The number of available processors/CPU cores on the current system.
     */
    public static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Constant representing one kilobyte (1024 bytes).
     */
    public static final long ONE_KB = 1024;

    /**
     * Constant representing one megabyte (1024 kilobytes).
     */
    public static final long ONE_MB = 1024 * ONE_KB;

    /**
     * Constant representing one gigabyte (1024 megabytes).
     */
    public static final long ONE_GB = 1024 * ONE_MB;

    /**
     * Constant representing one terabyte (1024 gigabytes).
     */
    public static final long ONE_TB = 1024 * ONE_GB;

    /**
     * Constant representing one petabyte (1024 terabytes).
     */
    public static final long ONE_PB = 1024 * ONE_TB;

    /**
     * Constant representing one exabyte (1024 petabytes).
     */
    public static final long ONE_EB = 1024 * ONE_PB;

    // public static final long ONE_ZB = 1024 * ONE_EB; // overflow

    /**
     * The maximum memory available to the JVM in megabytes.
     */
    public static final int MAX_MEMORY_IN_MB = (int) (Runtime.getRuntime().maxMemory() / (1024 * 1024));

    // ...
    /**
     * The operating system name.
     */
    public static final String OS_NAME = System.getProperty("os.name");

    /**
     * The operating system version.
     */
    public static final String OS_VERSION = System.getProperty("os.version");

    /**
     * The operating system architecture.
     */
    public static final String OS_ARCH = System.getProperty("os.arch");

    //...
    /**
     * Flag indicating whether the current operating system is Windows.
     */
    public static final boolean IS_OS_WINDOWS = OS_NAME.toUpperCase().contains("WINDOWS");

    /**
     * Flag indicating whether the current operating system is Mac.
     */
    public static final boolean IS_OS_MAC = OS_NAME.toUpperCase().contains("MAC");

    /**
     * Flag indicating whether the current operating system is Mac OS X.
     */
    public static final boolean IS_OS_MAC_OSX = OS_NAME.toUpperCase().contains("MAC OS X");

    /**
     * Flag indicating whether the current operating system is Linux.
     */
    public static final boolean IS_OS_LINUX = OS_NAME.toUpperCase().contains("LINUX");

    /**
     * Flag indicating whether the current platform is Android.
     */
    public static final boolean IS_PLATFORM_ANDROID = System.getProperty(JAVA_VENDOR_STR).toUpperCase().contains(ANDROID)
            || System.getProperty(JAVA_VM_VENDOR_STR).toUpperCase().contains(ANDROID);

    // ...
    /**
     * The Java home directory.
     */
    public static final String JAVA_HOME = System.getProperty("java.home");

    /**
     * The Java version as a JavaVersion object.
     */
    public static final JavaVersion JAVA_VERSION = JavaVersion.of(System.getProperty("java.version"));

    /**
     * The Java vendor name.
     */
    public static final String JAVA_VENDOR = System.getProperty(JAVA_VENDOR_STR);

    /**
     * The Java class path.
     */
    public static final String JAVA_CLASS_PATH = System.getProperty("java.class.path");

    /**
     * The Java class version.
     */
    public static final String JAVA_CLASS_VERSION = System.getProperty("java.class.version");

    /**
     * The Java runtime name.
     */
    public static final String JAVA_RUNTIME_NAME = System.getProperty("java.runtime.name");

    /**
     * The Java runtime version.
     */
    public static final String JAVA_RUNTIME_VERSION = System.getProperty("java.runtime.version");

    /**
     * The Java specification name.
     */
    public static final String JAVA_SPECIFICATION_NAME = System.getProperty("java.specification.name");

    /**
     * The Java specification vendor.
     */
    public static final String JAVA_SPECIFICATION_VENDOR = System.getProperty("java.specification.vendor");

    /**
     * The Java specification version.
     */
    public static final String JAVA_SPECIFICATION_VERSION = System.getProperty("java.specification.version");

    /**
     * The Java Virtual Machine implementation info.
     */
    public static final String JAVA_VM_INFO = System.getProperty("java.vm.info");

    /**
     * The Java Virtual Machine implementation name.
     */
    public static final String JAVA_VM_NAME = System.getProperty("java.vm.name");

    /**
     * The Java Virtual Machine specification name.
     */
    public static final String JAVA_VM_SPECIFICATION_NAME = System.getProperty("java.vm.specification.name");

    /**
     * The Java Virtual Machine specification vendor.
     */
    public static final String JAVA_VM_SPECIFICATION_VENDOR = System.getProperty("java.vm.specification.vendor");

    /**
     * The Java Virtual Machine specification version.
     */
    public static final String JAVA_VM_SPECIFICATION_VERSION = System.getProperty("java.vm.specification.version");

    /**
     * The Java Virtual Machine implementation vendor.
     */
    public static final String JAVA_VM_VENDOR = System.getProperty(JAVA_VM_VENDOR_STR);

    /**
     * The Java Virtual Machine implementation version.
     */
    public static final String JAVA_VM_VERSION = System.getProperty("java.vm.version");

    /**
     * The Java temporary directory.
     */
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
    /**
     * The user's current working directory.
     */
    public static final String USER_DIR = System.getProperty("user.dir");

    /**
     * The user's home directory.
     */
    public static final String USER_HOME = System.getProperty("user.home");

    /**
     * The user's account name.
     */
    public static final String USER_NAME = System.getProperty("user.name");

    /**
     * The user's timezone.
     */
    public static final String USER_TIMEZONE = System.getProperty("user.timezone");

    /**
     * The user's language.
     */
    public static final String USER_LANGUAGE = System.getProperty("user.language");

    /**
     * The user's country or region.
     */
    public static final String USER_COUNTRY = System.getProperty("user.country") == null ? System.getProperty("user.region")
            : System.getProperty("user.country");

    /**
     * The system-dependent path separator character represented as a string.
     * On UNIX systems, this is ":"; on Microsoft Windows systems, it is ";".
     *
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
     * The system-dependent line separator string.
     * On UNIX systems, it is "\n"; on Microsoft Windows systems, it is "\r\n".
     *
     * @see System#lineSeparator()
     */
    public static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * The Unix line separator string ("\n").
     * @see System#lineSeparator()
     */
    public static final String LINE_SEPARATOR_UNIX = "\n";

    /**
     * The Windows line separator string ("\r\n").
     * @see System#lineSeparator()
     * @deprecated use {@link #LINE_SEPARATOR_UNIX} instead. It's recommended to use '\n' as the line separator in all platforms(?).
     * It will make things easier when files are shared between different OS platforms. Windows can handle '\n' correctly. 
     */
    public static final String LINE_SEPARATOR_WINDOWS = "\r\n";

    /**
     * current path retrieved by {@code new File("./").getAbsolutePath()}
     */
    public static final String CURRENT_DIR;

    static {
        final String path = new File("./").getAbsolutePath();
        CURRENT_DIR = path.charAt(path.length() - 1) == '.' ? path.substring(0, path.length() - 1) : path;
    }

    // ...
    /**
     * Constant representing End-Of-File (or stream).
     */
    public static final int EOF = -1;

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> all_files_filter = (parentDir, file) -> true;

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> directories_excluded_filter = (parentDir, file) -> !file.isDirectory();

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> directories_only_filter = (parentDir, file) -> file.isDirectory();

    private IOUtil() {
        // no instance;
    }

    /**
     * Retrieves the host name of the local machine.
     * if the host name cannot be determined, it returns "UNKNOWN_HOST_NAME".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hostName = IOUtil.getHostName();
     * System.out.println("Current host name: " + hostName);
     * }</pre>
     *
     * @return the host name of the local machine, or "UNKNOWN_HOST_NAME" if it cannot be determined
     */
    public static String getHostName() {
        String ret = hostName;

        if (ret == null) {
            // This may be slow on some machine. Move it from static initialization block to method.

            final boolean IS_PLATFORM_ANDROID = System.getProperty(JAVA_VENDOR_STR).toUpperCase().contains(ANDROID) //NOSONAR
                    || System.getProperty(JAVA_VM_VENDOR_STR).toUpperCase().contains(ANDROID); //NOSONAR

            // implementation for android support
            if (IS_PLATFORM_ANDROID) {
                ExecutorService executor = null;
                try {
                    executor = Executors.newSingleThreadExecutor();
                    ret = executor.submit(() -> InetAddress.getLocalHost().getHostName()).get();
                } catch (final Exception e) {
                    logger.error("Failed to get host name");
                } finally {
                    if (executor != null) {
                        executor.shutdown();
                    }
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
     * Returns the free disk space on the volume where the current working directory resides, in kilobytes (KB).
     * This is equivalent to calling {@code freeDiskSpaceKb(new File(".").getAbsolutePath())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     long freeSpace = IOUtil.freeDiskSpaceKb();
     *     System.out.println("Free disk space: " + freeSpace + " KB");
     * } catch (UncheckedIOException e) {
     *     System.err.println("Failed to get free disk space: " + e.getMessage());
     * }
     * }</pre>
     *
     * @return the amount of free disk space in kilobytes
     * @throws UncheckedIOException if an I/O error occurs while retrieving the free space information
     */
    public static long freeDiskSpaceKb() throws UncheckedIOException {
        try {
            return FileSystemUtil.freeSpaceKb();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the free disk space on the volume where the current working directory resides, in kilobytes (KB).
     * This is equivalent to calling {@code freeDiskSpaceKb(new File(".").getAbsolutePath(), timeout)}.
     * The command to retrieve the disk space will be aborted if it exceeds the specified timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     long freeSpace = IOUtil.freeDiskSpaceKb(5000); // 5-second timeout
     *     System.out.println("Free disk space: " + freeSpace + " KB");
     * } catch (UncheckedIOException e) {
     *     System.err.println("Failed to get free disk space within the specified timeout: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param timeout the maximum time in milliseconds to wait for the command to complete. A value of zero or less means no timeout
     * @return the amount of free disk space in kilobytes
     * @throws UncheckedIOException if an I/O error occurs or the command times out
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
     * Returns the free disk space on the specified path in kilobytes (KB).
     * The free space is determined by invoking a command-line utility appropriate for the operating system.
     * <ul>
     *     <li>On Windows, it uses {@code dir /-c}.</li>
     *     <li>On AIX/HP-UX, it uses {@code df -kP}.</li>
     *     <li>On other Unix-based systems, it uses {@code df -k}.</li>
     * </ul>
     * Note: The accuracy of this method depends on the availability and output format of the underlying system commands.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     // For Windows
     *     long freeSpace = IOUtil.freeDiskSpaceKb("C:\\");
     *     System.out.println("Free space on C: " + freeSpace + " KB");
     * 
     *     // For Unix-like systems
     *     long freeSpaceUnix = IOUtil.freeDiskSpaceKb("/home");
     *     System.out.println("Free space on /home: " + freeSpaceUnix + " KB");
     * } catch (UncheckedIOException e) {
     *     System.err.println("Failed to get free disk space for the specified path: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param path the path to a file or directory on the volume to check. It must not be {@code null}. On Unix, it should not be an empty string.
     * @return the amount of free disk space in kilobytes.
     * @throws UncheckedIOException if an I/O error occurs while retrieving the free space information.
     * @throws IllegalArgumentException if the path is invalid.
     */
    public static long freeDiskSpaceKb(final String path) throws UncheckedIOException {
        try {
            return FileSystemUtil.freeSpaceKb(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the free disk space on the specified path in kilobytes (KB), with a timeout for the operation.
     * The free space is determined by invoking a command-line utility appropriate for the operating system, and the command will be aborted if it exceeds the specified timeout.
     * <ul>
     *     <li>On Windows, it uses {@code dir /-c}.</li>
     *     <li>On AIX/HP-UX, it uses {@code df -kP}.</li>
     *     <li>On other Unix-based systems, it uses {@code df -k}.</li>
     * </ul>
     * Note: The accuracy of this method depends on the availability and output format of the underlying system commands.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     // For Windows, with a 5-second timeout
     *     long freeSpace = IOUtil.freeDiskSpaceKb("C:\\", 5000);
     *     System.out.println("Free space on C: " + freeSpace + " KB");
     * } catch (UncheckedIOException e) {
     *     System.err.println("Failed to get free disk space for the specified path within the timeout: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param path the path to a file or directory on the volume to check. It must not be {@code null}. On Unix, it should not be an empty string.
     * @param timeout the maximum time in milliseconds to wait for the command to complete. A value of zero or less means no timeout.
     * @return the amount of free disk space in kilobytes.
     * @throws UncheckedIOException if an I/O error occurs or the command times out.
     * @throws IllegalArgumentException if the path is invalid.
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
     * Converts a character array to a byte array using the default character set of the platform.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = new char[] {'H', 'e', 'l', 'l', 'o'};
     * byte[] bytes = IOUtil.chars2Bytes(chars);
     * }</pre>
     *
     * @param chars the character array to convert. May be {@code null} or empty.
     * @return the resulting byte array, or an empty byte array if the input is {@code null} or empty.
     */
    public static byte[] chars2Bytes(final char[] chars) {
        return chars2Bytes(chars, DEFAULT_CHARSET);
    }

    /**
     * Converts a character array to a byte array using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = new char[] {'H', 'e', 'l', 'l', 'o'};
     * byte[] bytes = IOUtil.chars2Bytes(chars, StandardCharsets.UTF_8);
     * IOUtil.chars2Bytes(null); // returns empty byte array
     * }</pre>
     *
     * @param chars the character array to convert. May be {@code null} or empty.
     * @param charset the character set to use for encoding. If {@code null}, the platform's default charset is used.
     * @return the resulting byte array, or an empty byte array if the input is {@code null} or empty.
     */
    public static byte[] chars2Bytes(final char[] chars, final Charset charset) {
        if (N.isEmpty(chars)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return chars2Bytes(chars, 0, chars.length, charset);
    }

    /**
     * Converts a sub-array of a character array to a byte array using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = new char[] {'H', 'e', 'l', 'l', 'o'};
     * // Convert the sub-array {'e', 'l', 'l'}
     * byte[] bytes = IOUtil.chars2Bytes(chars, 1, 3, StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param chars the source character array, may be {@code null} or empty
     * @param offset the starting position in the character array (0-based), must be &gt;= 0
     * @param charCount the number of characters to convert, must be &gt;= 0
     * @param charset the character set to use for encoding. If {@code null}, the platform's default charset is used.
     * @return the resulting byte array, or an empty byte array if {@code charCount} is zero.
     * @throws IndexOutOfBoundsException if {@code offset} or {@code charCount} is out of bounds.
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
     * Converts a byte array to a character array using the default character set of the platform.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = new byte[] {72, 101, 108, 108, 111}; // "Hello"
     * char[] chars = IOUtil.bytes2Chars(bytes);
     * IOUtil.bytes2Chars(null); // returns empty char array
     * }</pre>
     *
     * @param bytes the byte array to convert. May be {@code null} or empty.
     * @return the resulting character array, or an empty character array if the input is {@code null} or empty.
     */
    public static char[] bytes2Chars(final byte[] bytes) {
        return bytes2Chars(bytes, DEFAULT_CHARSET);
    }

    /**
     * Converts a byte array to a character array using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = new byte[] {72, 101, 108, 108, 111}; // "Hello" in UTF-8
     * char[] chars = IOUtil.bytes2Chars(bytes, StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param bytes the byte array to convert. May be {@code null} or empty.
     * @param charset the character set to use for decoding. If {@code null}, the platform's default charset is used.
     * @return the resulting character array, or an empty character array if the input is {@code null} or empty.
     */
    public static char[] bytes2Chars(final byte[] bytes, final Charset charset) {
        if (N.isEmpty(bytes)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return bytes2Chars(bytes, 0, bytes.length, charset);
    }

    /**
     * Converts a sub-array of a byte array to a character array using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = new byte[] {72, 101, 108, 108, 111}; // "Hello" in UTF-8
     * // Convert the sub-array {101, 108, 108}
     * char[] chars = IOUtil.bytes2Chars(bytes, 1, 3, StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param bytes the source byte array, may be {@code null} or empty
     * @param offset the starting position in the byte array (0-based), must be &gt;= 0
     * @param byteCount the number of bytes to convert, must be &gt;= 0
     * @param charset the character set to use for decoding. If {@code null}, the platform's default charset is used.
     * @return the resulting character array, or an empty character array if {@code byteCount} is zero.
     * @throws IndexOutOfBoundsException if {@code offset} or {@code byteCount} is out of bounds.
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
     * Converts a {@code String} to an {@code InputStream} using the platform's default character set.
     * if the input string is {@code null}, an empty {@code InputStream} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String str = "Hello, World!";
     * try (InputStream inputStream = IOUtil.string2InputStream(str)) {
     *     // Use the inputStream
     * }
     * }</pre>
     *
     * @param str the string to convert, must not be {@code null}
     * @return an {@code InputStream} for the given string, or an empty {@code InputStream} if the input is {@code null}.
     */
    public static InputStream string2InputStream(final String str) {
        return string2InputStream(str, DEFAULT_CHARSET);
    }

    /**
     * Converts a {@code String} to an {@code InputStream} using the specified character set.
     * if the input string is {@code null}, an empty {@code InputStream} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String str = "Hello, World!";
     * try (InputStream inputStream = IOUtil.string2InputStream(str, StandardCharsets.UTF_8)) {
     *     // Use the inputStream
     * }
     * }</pre>
     *
     * @param str the string to convert, must not be {@code null}
     * @param charset the character set to use for encoding. If {@code null}, the platform's default charset is used.
     * @return an {@code InputStream} for the given string, or an empty {@code InputStream} if the input is {@code null}.
     */
    public static InputStream string2InputStream(final String str, Charset charset) {
        charset = checkCharset(charset);

        final byte[] bytes = N.isEmpty(str) ? N.EMPTY_BYTE_ARRAY : chars2Bytes(str.toCharArray(), charset);

        return new ByteArrayInputStream(bytes);
    }

    /**
     * Converts a {@code String} into a {@code Reader}.
     * if the input string is {@code null}, an empty reader is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String str = "Hello, World!";
     * try (Reader reader = IOUtil.string2Reader(str)) {
     *     // Use the reader
     * }
     * }</pre>
     *
     * @param str the string to convert. May be {@code null}.
     * @return a {@code Reader} for the given string, or an empty reader if the input is {@code null}.
     * @see StringReader
     */
    public static Reader string2Reader(final String str) {
        return new StringReader(Strings.nullToEmpty(str));
    }

    /**
     * Wraps a {@code StringBuilder} in a {@code Writer}.
     * Any characters written to the writer will be appended to the string builder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * try (Writer writer = IOUtil.stringBuilder2Writer(sb)) {
     *     writer.write("Hello");
     * }
     * // sb.toString() will be "Hello"
     * }</pre>
     *
     * @param sb the {@code StringBuilder} to wrap, must not be {@code null}
     * @return a {@code Writer} that appends to the specified string builder.
     * @throws IllegalArgumentException if the input {@code StringBuilder} is {@code null}.
     */
    public static Writer stringBuilder2Writer(final StringBuilder sb) throws IllegalArgumentException {
        if (sb == null) {
            throw new IllegalArgumentException("The input StringBuilder can't be null.");
        }

        return new StringWriter(sb);
    }

    /**
     * Reads all bytes from a file into a byte array.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * try {
     *     byte[] fileBytes = IOUtil.readAllBytes(file);
     *     // Process the bytes
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @return a byte array containing all bytes from the file
     * @throws OutOfMemoryError if the file is too large to be read into a byte array
     * @throws UncheckedIOException if an I/O error occurs
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
     * Reads all remaining bytes from an {@code InputStream} into a byte array.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("data.bin")) {
     *     byte[] allBytes = IOUtil.readAllBytes(inputStream);
     *     // Process the bytes
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @return a byte array containing all bytes read from the stream
     * @throws OutOfMemoryError if the stream is too large to be read into a byte array
     * @throws UncheckedIOException if an I/O error occurs
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
     * This method is similar to {@link #readAllBytes(File)} but throws a checked {@code IOException}.
     * It handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * try {
     *     byte[] fileBytes = IOUtil.readBytes(file);
     *     // Process the bytes
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @return a byte array containing all bytes from the file.
     * @throws OutOfMemoryError if the file is too large to be read into a byte array.
     * @throws IOException if an I/O error occurs.
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
     * Reads up to a specified number of bytes from a file into a byte array, starting from a given offset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * try {
     *     // Read 1024 bytes, starting from the 100th byte
     *     byte[] partialBytes = IOUtil.readBytes(file, 100, 1024);
     *     // Process the partial bytes
     * } catch (IOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param offset the starting position in bytes from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of bytes to read, must be &gt;= 0
     * @return a byte array containing the bytes read from the file. The length of the array will be at most {@code maxLen}
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
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
     * Reads all remaining bytes from an {@code InputStream} into a byte array.
     * This method is similar to {@link #readAllBytes(InputStream)} but throws a checked {@code IOException}.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("data.bin")) {
     *     byte[] allBytes = IOUtil.readBytes(inputStream);
     *     // Process the bytes
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @return a byte array containing all bytes read from the stream
     * @throws OutOfMemoryError if the stream is too large to be read into a byte array
     * @throws IOException if an I/O error occurs
     * @see #readAllBytes(InputStream)
     */
    @Beta
    public static byte[] readBytes(final InputStream source) throws IOException {
        return readBytes(source, 0, Long.MAX_VALUE);
    }

    /**
     * Reads up to a specified number of bytes from an {@code InputStream} into a byte array, starting from a given offset.
     * This method will skip the specified number of bytes from the stream before starting to read.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("data.bin")) {
     *     // Skip the first 100 bytes and read the next 1024 bytes
     *     byte[] partialBytes = IOUtil.readBytes(inputStream, 100, 1024);
     *     // Process the partial bytes
     * } catch (IOException e) {
     *     System.err.println("Error reading partial stream content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param offset the starting position in bytes from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of bytes to read, must be &gt;= 0
     * @return a byte array containing the bytes read from the stream. The length of the array will be at most {@code maxLen}
     * @throws IOException if an I/O error occurs during reading or skipping
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
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
     * Reads all characters from a file into a character array using the platform's default character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     char[] fileChars = IOUtil.readAllChars(file);
     *     // Process the characters
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @return a character array containing all characters from the file.
     * @throws OutOfMemoryError if the file is too large to be read into a character array.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static char[] readAllChars(final File source) throws UncheckedIOException {
        return readAllChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all characters from a file into a character array using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     char[] fileChars = IOUtil.readAllChars(file, StandardCharsets.UTF_8);
     *     // Process the characters
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return a character array containing all characters from the file
     * @throws OutOfMemoryError if the file is too large to be read into a character array
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static char[] readAllChars(final File source, final Charset charset) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readAllChars(is, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads all remaining characters from an {@code InputStream} into a character array using the platform's default character set.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     char[] allChars = IOUtil.readAllChars(inputStream);
     *     // Process the characters
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, may be {@code null}
     * @return a character array containing all characters read from the stream.
     * @throws OutOfMemoryError if the stream is too large to be read into a character array.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static char[] readAllChars(final InputStream source) throws UncheckedIOException {
        return readAllChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all remaining characters from an {@code InputStream} into a character array using the specified character set.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     char[] allChars = IOUtil.readAllChars(inputStream, StandardCharsets.UTF_8);
     *     // Process the characters
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return a character array containing all characters read from the stream
     * @throws OutOfMemoryError if the stream is too large to be read into a character array
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static char[] readAllChars(final InputStream source, final Charset charset) throws UncheckedIOException {
        final Reader reader = createReader(source, charset);

        try {
            return readChars(reader, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads all remaining characters from a {@code Reader} into a character array.
     * <p>
     * Note: This method should not be used for readers with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     char[] allChars = IOUtil.readAllChars(reader);
     *     // Process the characters
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, may be {@code null}
     * @return a character array containing all characters read from the reader.
     * @throws OutOfMemoryError if the reader's content is too large to be read into a character array.
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
     * Reads all characters from a file into a character array using the platform's default character set.
     * This method is similar to {@link #readAllChars(File)} but throws a checked {@code IOException}.
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     char[] fileChars = IOUtil.readChars(file);
     *     // Process the characters
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @return a character array containing all characters from the file.
     * @throws OutOfMemoryError if the file is too large to be read into a character array.
     * @throws IOException if an I/O error occurs.
     * @see #readAllChars(File)
     */
    @Beta
    public static char[] readChars(final File source) throws IOException {
        return readChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all characters from a file into a character array using the specified character set.
     * This method is similar to {@link #readAllChars(File, Charset)} but throws a checked {@code IOException}.
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     char[] fileChars = IOUtil.readChars(file, StandardCharsets.UTF_8);
     *     // Process the characters
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return a character array containing all characters from the file
     * @throws OutOfMemoryError if the file is too large to be read into a character array
     * @throws IOException if an I/O error occurs
     * @see #readAllChars(File, Charset)
     */
    @Beta
    public static char[] readChars(final File source, final Charset charset) throws IOException {
        return readChars(source, charset, 0, Integer.MAX_VALUE);
    }

    /**
     * Reads up to a specified number of characters from a file into a character array, starting from a given character offset, using the platform's default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 1024 characters, starting from the 100th character
     *     char[] partialChars = IOUtil.readChars(file, 100, 1024);
     *     // Process the partial characters
     * } catch (IOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a character array containing the characters read from the file. The length of the array will be at most {@code maxLen}
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     */
    public static char[] readChars(final File source, final long offset, final int maxLen) throws IOException {
        return readChars(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads up to a specified number of characters from a file into a character array, starting from a given character offset, using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 1024 characters, starting from the 100th character, using UTF-8 encoding
     *     char[] partialChars = IOUtil.readChars(file, StandardCharsets.UTF_8, 100, 1024);
     *     // Process the partial characters
     * } catch (IOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a character array containing the characters read from the file. The length of the array will be at most {@code maxLen}
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     */
    public static char[] readChars(final File source, final Charset charset, final long offset, final int maxLen) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readChars(is, charset, offset, maxLen);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads all remaining characters from an {@code InputStream} into a character array using the platform's default character set.
     * This method is similar to {@link #readAllChars(InputStream)} but throws a checked {@code IOException}.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     char[] allChars = IOUtil.readChars(inputStream);
     *     // Process the characters
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, may be {@code null}
     * @return a character array containing all characters read from the stream.
     * @throws OutOfMemoryError if the stream is too large to be read into a character array.
     * @throws IOException if an I/O error occurs.
     * @see #readAllChars(InputStream)
     * @see #readChars(InputStream, Charset)
     * @see #readChars(Reader)
     */
    @Beta
    public static char[] readChars(final InputStream source) throws IOException {
        return readChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all remaining characters from an {@code InputStream} into a character array using the specified character set.
     * This method is similar to {@link #readAllChars(InputStream, Charset)} but throws a checked {@code IOException}.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     char[] allChars = IOUtil.readChars(inputStream, StandardCharsets.UTF_8);
     *     // Process the characters
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return a character array containing all characters read from the stream
     * @throws OutOfMemoryError if the stream is too large to be read into a character array
     * @throws IOException if an I/O error occurs
     * @see #readAllChars(InputStream, Charset)
     * @see #readChars(Reader)
     */
    @Beta
    public static char[] readChars(final InputStream source, final Charset charset) throws IOException {
        return readChars(source, charset, 0, Integer.MAX_VALUE);
    }

    /**
     * Reads up to a specified number of characters from an {@code InputStream} into a character array, starting from a given character offset, using the platform's default charset.
     * This method will skip the specified number of bytes from the stream before starting to read.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     // Skip the first 100 characters and read the next 1024 characters
     *     char[] partialChars = IOUtil.readChars(inputStream, 100, 1024);
     *     // Process the partial characters
     * } catch (IOException e) {
     *     System.err.println("Error reading partial stream content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a character array containing the characters read from the stream. The length of the array will be at most {@code maxLen}
     * @throws IOException if an I/O error occurs during reading or skipping
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     * @see #readChars(InputStream, Charset, long, int)
     * @see #readChars(Reader, long, int)
     */
    public static char[] readChars(final InputStream source, final long offset, final int maxLen) throws IOException {
        return readChars(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads up to a specified number of characters from an {@code InputStream} into a character array, starting from a given character offset, using the specified character set.
     * This method will skip the specified number of bytes from the stream before starting to read.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     // Skip the first 100 characters and read the next 1024 characters using UTF-8
     *     char[] partialChars = IOUtil.readChars(inputStream, StandardCharsets.UTF_8, 100, 1024);
     *     // Process the partial characters
     * } catch (IOException e) {
     *     System.err.println("Error reading partial stream content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a character array containing the characters read from the stream. The length of the array will be at most {@code maxLen}
     * @throws IOException if an I/O error occurs during reading or skipping
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     * @see #readChars(Reader, long, int)
     */
    public static char[] readChars(final InputStream source, final Charset charset, final long offset, final int maxLen) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        // try {
        final Reader reader = createReader(source, checkCharset(charset));
        // } finally {
        // // close(reader);
        // }

        return readChars(reader, offset, maxLen);
    }

    /**
     * Reads all remaining characters from a {@code Reader} into a character array.
     * This method is similar to {@link #readAllChars(Reader)} but throws a checked {@code IOException}.
     * <p>
     * Note: This method should not be used for readers with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     char[] allChars = IOUtil.readChars(reader);
     *     // Process the characters
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, may be {@code null}
     * @return a character array containing all characters read from the reader.
     * @throws OutOfMemoryError if the reader's content is too large to be read into a character array.
     * @throws IOException if an I/O error occurs.
     * @see #readAllChars(Reader)
     */
    @Beta
    public static char[] readChars(final Reader source) throws IOException {
        return readChars(source, 0, Long.MAX_VALUE);
    }

    /**
     * Reads up to a specified number of characters from a {@code Reader} into a character array, starting from a given character offset.
     * This method will skip the specified number of characters from the reader before starting to read.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     // Skip the first 100 characters and read the next 1024 characters
     *     char[] partialChars = IOUtil.readChars(reader, 100, 1024);
     *     // Process the partial characters
     * } catch (IOException e) {
     *     System.err.println("Error reading partial reader content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a character array containing the characters read from the reader. The length of the array will be at most {@code maxLen}
     * @throws IOException if an I/O error occurs during reading or skipping
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
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
                    // Use long arithmetic to prevent integer overflow when calculating new capacity
                    long newCapacityLong = (long) (arrayLength * 1.75);
                    int newCapacity;

                    if (newCapacityLong < 0 || newCapacityLong > maxLen || newCapacityLong > N.MAX_ARRAY_SIZE) {
                        newCapacity = (int) N.min(maxLen, N.MAX_ARRAY_SIZE);
                    } else {
                        newCapacity = (int) newCapacityLong;
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
     * Reads the entire content of a file into a {@code String} using the platform's default character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String content = IOUtil.readAllToString(file);
     *     System.out.println(content);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @return a {@code String} containing the entire content of the file.
     * @throws OutOfMemoryError if the file is too large to be read into a string.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static String readAllToString(final File source) throws UncheckedIOException {
        return readAllToString(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the entire content of a file into a {@code String} using the specified character set name.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String content = IOUtil.readAllToString(file, "UTF-8");
     *     System.out.println(content);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param encoding the name of the character set to use for decoding. If {@code null}, the platform's default charset is used.
     * @return a {@code String} containing the entire content of the file.
     * @throws OutOfMemoryError if the file is too large to be read into a string.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws IllegalArgumentException if the named charset is not supported.
     */
    public static String readAllToString(final File source, final String encoding) throws UncheckedIOException {
        return readAllToString(source, Charsets.get(encoding));
    }

    /**
     * Reads the entire content of a file into a {@code String} using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String content = IOUtil.readAllToString(file, StandardCharsets.UTF_8);
     *     System.out.println(content);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return a {@code String} containing the entire content of the file
     * @throws OutOfMemoryError if the file is too large to be read into a string
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static String readAllToString(final File source, final Charset charset) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readAllToString(is, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads all remaining characters from an {@code InputStream} into a {@code String} using the platform's default character set.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     String content = IOUtil.readAllToString(inputStream);
     *     System.out.println(content);
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, may be {@code null}
     * @return a {@code String} containing all content read from the stream.
     * @throws OutOfMemoryError if the stream is too large to be read into a string.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #readAllToString(InputStream, Charset)
     * @see #readAllToString(Reader)
     */
    public static String readAllToString(final InputStream source) throws UncheckedIOException {
        return readAllToString(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all remaining characters from an {@code InputStream} into a {@code String} using the specified character set.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     String content = IOUtil.readAllToString(inputStream, StandardCharsets.UTF_8);
     *     System.out.println(content);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return a {@code String} containing all content read from the stream
     * @throws OutOfMemoryError if the stream is too large to be read into a string
     * @throws UncheckedIOException if an I/O error occurs
     * @see #readAllToString(Reader)
     */
    public static String readAllToString(final InputStream source, final Charset charset) throws UncheckedIOException {
        //    // try {
        //    final Reader reader = createReader(source, checkCharset(charset));
        //    // } finally {
        //    // // close(reader);
        //    // }
        //    return readAllToString(reader);

        // For better performance. But maybe overflow for large size of InputStream.
        // It may work for char[] as well but not for byte[].
        final byte[] bytes = readAllBytes(source);

        return new String(bytes, checkCharset(charset));
    }

    /**
     * Reads all remaining characters from a {@code Reader} into a {@code String}.
     * <p>
     * Note: This method should not be used for readers with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     String content = IOUtil.readAllToString(reader);
     *     System.out.println(content);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}
     * @return a {@code String} containing all content read from the reader
     * @throws OutOfMemoryError if the reader's content is too large to be read into a string
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static String readAllToString(final Reader source) throws UncheckedIOException {
        final char[] chars = readAllChars(source);

        return String.valueOf(chars);
    }

    /**
     * Reads up to a specified number of characters from a file into a {@code String}, starting from a given character offset, using the platform's default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 1024 characters, starting from the 100th character
     *     String partialContent = IOUtil.readToString(file, 100, 1024);
     *     System.out.println(partialContent);
     * } catch (IOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a {@code String} containing the characters read from the file
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     */
    public static String readToString(final File source, final long offset, final int maxLen) throws IOException {
        return readToString(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads up to a specified number of characters from a file into a {@code String}, starting from a given character offset, using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 1024 characters, starting from the 100th character, using UTF-8 encoding
     *     String partialContent = IOUtil.readToString(file, StandardCharsets.UTF_8, 100, 1024);
     *     System.out.println(partialContent);
     * } catch (IOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a {@code String} containing the characters read from the file
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     */
    public static String readToString(final File source, final Charset charset, final long offset, final int maxLen) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readToString(is, charset, offset, maxLen);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads up to a specified number of characters from an {@code InputStream} into a {@code String}, starting from a given character offset, using the platform's default charset.
     * This method will skip the specified number of bytes from the stream before starting to read.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     // Skip the first 100 characters and read the next 1024 characters
     *     String partialContent = IOUtil.readToString(inputStream, 100, 1024);
     *     System.out.println(partialContent);
     * } catch (IOException e) {
     *     System.err.println("Error reading partial stream content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a {@code String} containing the characters read from the stream
     * @throws IOException if an I/O error occurs during reading or skipping
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     * @see #readToString(InputStream, Charset, long, int)
     * @see #readToString(Reader, long, int)
     */
    public static String readToString(final InputStream source, final long offset, final int maxLen) throws IOException {
        return readToString(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads up to a specified number of characters from an {@code InputStream} into a {@code String}, starting from a given character offset, using the specified character set.
     * This method will skip the specified number of bytes from the stream before starting to read.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     // Skip the first 100 characters and read the next 1024 characters using UTF-8
     *     String partialContent = IOUtil.readToString(inputStream, StandardCharsets.UTF_8, 100, 1024);
     *     System.out.println(partialContent);
     * } catch (IOException e) {
     *     System.err.println("Error reading partial stream content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a {@code String} containing the characters read from the stream
     * @throws IOException if an I/O error occurs during reading or skipping
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     * @see #readToString(Reader, long, int)
     */
    public static String readToString(final InputStream source, final Charset charset, final long offset, final int maxLen) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        // try {
        final Reader reader = createReader(source, checkCharset(charset));
        // } finally {
        // // close(reader);
        // }

        return readToString(reader, offset, maxLen);
    }

    /**
     * Reads up to a specified number of characters from a {@code Reader} into a {@code String}, starting from a given character offset.
     * This method will skip the specified number of characters from the reader before starting to read.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     // Skip the first 100 characters and read the next 1024 characters
     *     String partialContent = IOUtil.readToString(reader, 100, 1024);
     *     System.out.println(partialContent);
     * } catch (IOException e) {
     *     System.err.println("Error reading partial reader content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param maxLen the maximum number of characters to read, must be &gt;= 0
     * @return a {@code String} containing the characters read from the reader
     * @throws IOException if an I/O error occurs during reading or skipping
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative
     */
    public static String readToString(final Reader source, final long offset, final int maxLen) throws IOException {
        final char[] chs = readChars(source, offset, maxLen);

        return String.valueOf(chs);
    }

    /**
     * Reads all lines from a file into a list of strings, using the platform's default charset.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     List<String> lines = IOUtil.readAllLines(file);
     *     lines.forEach(System.out::println);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @return a list of strings, each representing a line in the file.
     * @throws OutOfMemoryError if the file is too large to be read into memory.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static List<String> readAllLines(final File source) throws UncheckedIOException {
        return readAllLines(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all lines from a file into a list of strings, using the specified character set name.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     List<String> lines = IOUtil.readAllLines(file, "UTF-8");
     *     lines.forEach(System.out::println);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param encoding the name of the character set to use for decoding. If {@code null}, the platform's default charset is used.
     * @return a list of strings, each representing a line in the file.
     * @throws OutOfMemoryError if the file is too large to be read into memory.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws IllegalArgumentException if the named charset is not supported.
     */
    public static List<String> readAllLines(final File source, final String encoding) throws UncheckedIOException {
        return readAllLines(source, Charsets.get(encoding));
    }

    /**
     * Reads all lines from a file into a list of strings, using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first entry).
     * <p>
     * Note: This method should not be used for files with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     List<String> lines = IOUtil.readAllLines(file, StandardCharsets.UTF_8);
     *     lines.forEach(System.out::println);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return a list of strings, each representing a line in the file
     * @throws OutOfMemoryError if the file is too large to be read into memory
     * @throws UncheckedIOException if an I/O error occurs
     */
    public static List<String> readAllLines(final File source, final Charset charset) throws UncheckedIOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readAllLines(is, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads all lines from an {@code InputStream} into a list of strings, using the platform's default character set.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     List<String> lines = IOUtil.readAllLines(inputStream);
     *     lines.forEach(System.out::println);
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, may be {@code null}
     * @return a list of strings, each representing a line from the stream.
     * @throws OutOfMemoryError if the stream is too large to be read into memory.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #readAllLines(InputStream, Charset)
     * @see #readAllLines(Reader)
     */
    public static List<String> readAllLines(final InputStream source) throws UncheckedIOException {
        return readAllLines(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all lines from an {@code InputStream} into a list of strings, using the specified character set.
     * <p>
     * Note: This method should not be used for streams with a size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     List<String> lines = IOUtil.readAllLines(inputStream, StandardCharsets.UTF_8);
     *     lines.forEach(System.out::println);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return a list of strings, each representing a line from the stream
     * @throws OutOfMemoryError if the stream is too large to be read into memory
     * @throws UncheckedIOException if an I/O error occurs
     * @see #readAllLines(Reader)
     */
    public static List<String> readAllLines(final InputStream source, final Charset charset) throws UncheckedIOException {

        // try {
        final Reader reader = createReader(source, charset);
        // } finally {
        // // close(reader);
        // }

        return readAllLines(reader);
    }

    /**
     * Reads all lines from a {@code Reader} into a list of strings.
     * <p>
     * Note: This method should not be used for readers with content size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     List<String> lines = IOUtil.readAllLines(reader);
     *     lines.forEach(System.out::println);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}
     * @return a list of strings, each representing a line from the reader
     * @throws OutOfMemoryError if the reader's content is too large to be read into memory
     * @throws UncheckedIOException if an I/O error occurs
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
     * Reads a specified number of lines from a file into a list of strings, starting from a given line offset, using the platform's default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 10 lines, starting from the 5th line
     *     List<String> lines = IOUtil.readLines(file, 4, 10);
     *     lines.forEach(System.out::println);
     * } catch (IOException e) {
     *     System.err.println("Error reading lines from file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param offset the 0-based index of the first line to read, must be &gt;= 0
     * @param count the number of lines to read, must be &gt;= 0
     * @return a list of strings, each representing a line from the specified range in the file
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative
     */
    public static List<String> readLines(final File source, final int offset, final int count) throws IOException {
        return readLines(source, DEFAULT_CHARSET, offset, count);
    }

    /**
     * Reads a specified number of lines from a file into a list of strings, starting from a given line offset, using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 10 lines, starting from the 5th line, using UTF-8 encoding
     *     List<String> lines = IOUtil.readLines(file, StandardCharsets.UTF_8, 4, 10);
     *     lines.forEach(System.out::println);
     * } catch (IOException e) {
     *     System.err.println("Error reading lines from file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @param offset the 0-based index of the first line to read, must be &gt;= 0
     * @param count the number of lines to read, must be &gt;= 0
     * @return a list of strings, each representing a line from the specified range in the file
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative
     */
    public static List<String> readLines(final File source, final Charset charset, final int offset, final int count) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readLines(is, charset, offset, count);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads a specified number of lines from an {@code InputStream} into a list of strings, starting from a given line offset, using the platform's default charset.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     // Read 10 lines, starting from the 5th line
     *     List<String> lines = IOUtil.readLines(inputStream, 4, 10);
     *     lines.forEach(System.out::println);
     * } catch (IOException e) {
     *     System.err.println("Error reading lines from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param offset the 0-based index of the first line to read, must be &gt;= 0
     * @param count the number of lines to read, must be &gt;= 0
     * @return a list of strings, each representing a line from the specified range in the stream
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative.
     * @see #readLines(InputStream, Charset, int, int)
     * @see #readLines(Reader, int, int)
     */
    public static List<String> readLines(final InputStream source, final int offset, final int count) throws IOException {
        return readLines(source, DEFAULT_CHARSET, offset, count);
    }

    /**
     * Reads a specified number of lines from an {@code InputStream} into a list of strings, starting from a given line offset, using the specified character set.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("text_file.txt")) {
     *     // Read 10 lines, starting from the 5th line, using UTF-8 encoding
     *     List<String> lines = IOUtil.readLines(inputStream, StandardCharsets.UTF_8, 4, 10);
     *     lines.forEach(System.out::println);
     * } catch (IOException e) {
     *     System.err.println("Error reading lines from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @param offset the 0-based index of the first line to read, must be &gt;= 0
     * @param count the number of lines to read, must be &gt;= 0
     * @return a list of strings, each representing a line from the specified range in the stream
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative
     * @see #readLines(Reader, int, int)
     */
    public static List<String> readLines(final InputStream source, final Charset charset, final int offset, final int count) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        return readLines(createReader(source, charset), offset, count);
    }

    /**
     * Reads a specified number of lines from a {@code Reader} into a list of strings, starting from a given line offset.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     // Read 10 lines, starting from the 5th line
     *     List<String> lines = IOUtil.readLines(reader, 4, 10);
     *     lines.forEach(System.out::println);
     * } catch (IOException e) {
     *     System.err.println("Error reading lines from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}
     * @param offset the 0-based index of the first line to read, must be &gt;= 0
     * @param count the number of lines to read, must be &gt;= 0
     * @return a list of strings, each representing a line from the specified range in the reader
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative
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
     * Reads the first line from a file using the platform's default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String firstLine = IOUtil.readFirstLine(file);
     *     if (firstLine != null) {
     *         System.out.println("First line: " + firstLine);
     *     }
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @return the first line of the file, or {@code null} if the file is empty.
     * @throws IOException if an I/O error occurs.
     */
    @MayReturnNull
    public static String readFirstLine(final File source) throws IOException {
        return readFirstLine(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the first line from a file using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String firstLine = IOUtil.readFirstLine(file, StandardCharsets.UTF_8);
     *     if (firstLine != null) {
     *         System.out.println("First line: " + firstLine);
     *     }
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return the first line of the file, or {@code null} if the file is empty
     * @throws IOException if an I/O error occurs
     */
    @MayReturnNull
    public static String readFirstLine(final File source, final Charset charset) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readFirstLine(createReader(is, charset));
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads the first line from a {@code Reader}.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     String firstLine = IOUtil.readFirstLine(reader);
     *     if (firstLine != null) {
     *         System.out.println("First line: " + firstLine);
     *     }
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}
     * @return the first line from the reader, or {@code null} if the reader is empty
     * @throws IOException if an I/O error occurs
     */
    @MayReturnNull
    public static String readFirstLine(final Reader source) throws IOException {
        return readLine(source, 0);
    }

    /**
     * Reads the last line from a file using the platform's default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String lastLine = IOUtil.readLastLine(file);
     *     if (lastLine != null) {
     *         System.out.println("Last line: " + lastLine);
     *     }
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @return the last line of the file, or {@code null} if the file is empty
     * @throws IOException if an I/O error occurs
     */
    @MayReturnNull
    public static String readLastLine(final File source) throws IOException {
        return readLastLine(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the last line from a file using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String lastLine = IOUtil.readLastLine(file, StandardCharsets.UTF_8);
     *     if (lastLine != null) {
     *         System.out.println("Last line: " + lastLine);
     *     }
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @return the last line of the file, or {@code null} if the file is empty
     * @throws IOException if an I/O error occurs
     */
    @MayReturnNull
    public static String readLastLine(final File source, final Charset charset) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readLastLine(createReader(is, charset));
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads the last line from a {@code Reader} and returns it as a {@code String}.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     String lastLine = IOUtil.readLastLine(reader);
     *     if (lastLine != null) {
     *         System.out.println("Last line: " + lastLine);
     *     }
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read the last line from, must not be {@code null}
     * @return a {@code String} containing the last line from the reader, or {@code null} if the reader is empty
     * @throws IOException if an I/O error occurs
     */
    @MayReturnNull
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
     * Reads a specific line from a file using the default charset and returns it as a {@code String}.
     * The line to read is determined by the provided line index (0-based).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String line = IOUtil.readLine(file, 0); // Read the first line
     *     System.out.println("First line: " + line);
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source    the file to read the line from, must not be {@code null}
     * @param lineIndex the index of the line to read, starting from 0 for the first line.
     * @return a {@code String} containing the specified line from the file, or {@code null} if the file has fewer lines.
     * @throws IllegalArgumentException if the line index is negative.
     * @throws IOException              if an I/O error occurs.
     */
    @MayReturnNull
    public static String readLine(final File source, final int lineIndex) throws IllegalArgumentException, IOException {
        return readLine(source, DEFAULT_CHARSET, lineIndex);
    }

    /**
     * Reads a specific line from a file using the specified character set and returns it as a {@code String}.
     * The line to read is determined by the provided line index (0-based).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String line = IOUtil.readLine(file, StandardCharsets.UTF_8, 5); // Read the 6th line
     *     System.out.println("Line 6: " + line);
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param charset the character set to use for decoding, if {@code null} the platform's default charset is used
     * @param lineIndex the index of the line to read, starting from 0 for the first line, must be &gt;= 0
     * @return a {@code String} containing the specified line, or {@code null} if the file has fewer lines
     * @throws IllegalArgumentException if {@code lineIndex} is negative
     * @throws IOException if an I/O error occurs
     */
    @MayReturnNull
    public static String readLine(final File source, final Charset charset, final int lineIndex) throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(lineIndex, cs.lineIndex);

        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return readLine(createReader(is, charset), lineIndex);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads a specific line from a {@code Reader} and returns it as a {@code String}.
     * The line to read is determined by the provided line index (0-based). The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     String line = IOUtil.readLine(reader, 4); // Read the 5th line
     *     System.out.println("Line 5: " + line);
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source    the {@code Reader} to read the line from, must not be {@code null}
     * @param lineIndex the index of the line to read, starting from 0 for the first line.
     * @return a {@code String} containing the specified line from the reader, or {@code null} if the reader has fewer lines.
     * @throws IllegalArgumentException if the line index is negative.
     * @throws IOException              if an I/O error occurs.
     */
    @MayReturnNull
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
            //        throw new IndexOutOfBoundsException("lineIndex: " + lineIndex + " exceeded the total line count of the specified reader/file"); // Should throw IllegalArgumentException
            //    }

            return br.readLine(); //NOSONAR
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Reads data from a file into a byte array buffer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * byte[] buffer = new byte[1024];
     * try {
     *     int bytesRead = IOUtil.read(file, buffer);
     *     System.out.println("Read " + bytesRead + " bytes");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}
     * @param buf    the byte array buffer where the data is to be stored, must not be {@code null}
     * @return the total number of bytes read into the buffer, or {@code -1} if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final byte[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a byte array buffer with specified offset and length.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * byte[] buffer = new byte[1024];
     * try {
     *     int bytesRead = IOUtil.read(file, buffer, 10, 100); // Read 100 bytes into buffer starting at index 10
     *     System.out.println("Read " + bytesRead + " bytes");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read data from, must not be {@code null}
     * @param buf    the byte array buffer where the data is to be stored, must not be {@code null}
     * @param off    the start offset in the array at which the data is written.
     * @param len    the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the file has been reached.
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
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("data.bin")) {
     *     byte[] buffer = new byte[1024];
     *     int bytesRead = IOUtil.read(inputStream, buffer);
     *     System.out.println("Read " + bytesRead + " bytes");
     * } catch (IOException e) {
     *     System.err.println("Error reading stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the InputStream to read data from, must not be {@code null}
     * @param buf    the byte array buffer where the data is to be stored, must not be {@code null}
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final InputStream source, final byte[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from an InputStream into a byte array buffer with specified offset and length.
     * The input stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream inputStream = new FileInputStream("data.bin")) {
     *     byte[] buffer = new byte[1024];
     *     int bytesRead = IOUtil.read(inputStream, buffer, 50, 200); // Read 200 bytes into buffer starting at index 50
     *     System.out.println("Read " + bytesRead + " bytes");
     * } catch (IOException e) {
     *     System.err.println("Error reading stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the InputStream to read data from, must not be {@code null}
     * @param buf    the byte array buffer where the data is to be stored, must not be {@code null}
     * @param off    the start offset in the array at which the data is written.
     * @param len    the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * char[] buffer = new char[1024];
     * try {
     *     int charsRead = IOUtil.read(file, buffer);
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read data from, must not be {@code null}
     * @param buf    the char array buffer where the data is to be stored, must not be {@code null}
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final char[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a char array buffer using the provided charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * char[] buffer = new char[1024];
     * try {
     *     int charsRead = IOUtil.read(file, StandardCharsets.UTF_8, buffer);
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source  the file to read data from, must not be {@code null}
     * @param charset the charset to be used to open the specified file for reading.
     * @param buf     the char array buffer where the data is to be stored, must not be {@code null}
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final Charset charset, final char[] buf) throws IOException {
        return read(source, charset, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a char array buffer using the default charset with specified offset and length.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * char[] buffer = new char[1024];
     * try {
     *     int charsRead = IOUtil.read(file, buffer, 10, 100); // Read 100 chars into buffer starting at index 10
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read data from, must not be {@code null}
     * @param buf    the char array buffer where the data is to be stored, must not be {@code null}
     * @param off    the start offset in the array at which the data is written.
     * @param len    the maximum number of chars to read.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final char[] buf, final int off, final int len) throws IOException {
        return read(source, DEFAULT_CHARSET, buf, off, len);
    }

    /**
     * Reads data from a file into a char array buffer using the provided charset with specified offset and length.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * char[] buffer = new char[1024];
     * try {
     *     int charsRead = IOUtil.read(file, StandardCharsets.UTF_8, buffer, 20, 150); // Read 150 chars starting at index 20
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source  the file to read data from, must not be {@code null}
     * @param charset the charset to be used to open the specified file for reading.
     * @param buf     the char array buffer where the data is to be stored, must not be {@code null}
     * @param off     the start offset in the array at which the data is written.
     * @param len     the maximum number of chars to read.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final File source, final Charset charset, final char[] buf, final int off, final int len) throws IOException {
        final Holder<ZipFile> outputZipFile = new Holder<>();
        InputStream is = null;

        try {
            is = openFile(source, outputZipFile);

            return read(createReader(is, charset), buf, off, len);
        } finally {
            close(is);
            close(outputZipFile.value());
        }
    }

    /**
     * Reads data from a Reader into a char array buffer.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     char[] buffer = new char[1024];
     *     int charsRead = IOUtil.read(reader, buffer);
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the Reader to read data from, must not be {@code null}
     * @param buf    the char array buffer where the data is to be stored, must not be {@code null}
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public static int read(final Reader source, final char[] buf) throws IOException {
        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from a Reader into a char array buffer with specified offset and length.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     char[] buffer = new char[1024];
     *     int charsRead = IOUtil.read(reader, buffer, 50, 200); // Read 200 chars into buffer starting at index 50
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the Reader to read data from, must not be {@code null}
     * @param buf    the char array buffer where the data is to be stored, must not be {@code null}
     * @param off    the start offset in the array at which the data is written.
     * @param len    the maximum number of chars to read.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
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

    static InputStreamReader createReader(final InputStream source, final Charset encoding) {
        return encoding == null ? IOUtil.newInputStreamReader(source, DEFAULT_CHARSET) : IOUtil.newInputStreamReader(source, encoding);
    }

    /**
     * Writes the string representation of an Object to a file as a single line using the default charset.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     *
     * @param obj    the Object to be written.
     * @param output the file where the object's string representation is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
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
     * Writes the string representation of an Object to a Writer as a single line using the default charset.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     *
     * @param obj    the Object to be written.
     * @param output the Writer where the object's string representation is to be written, must not be {@code null}
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
     * @param obj    the Object to be written.
     * @param output the Writer where the object's string representation is to be written, must not be {@code null}
     * @param flush  if {@code true}, the stream will be flushed after writing the line.
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void writeLine(final Object obj, final Writer output, final boolean flush) throws IOException {
        if (obj == null) {
            output.write(Strings.NULL_CHAR_ARRAY);
        } else {
            output.write(N.toString(obj));
        }

        output.write(IOUtil.LINE_SEPARATOR_UNIX);

        if (flush) {
            output.flush();
        }
    }

    /**
     * Writes the string representation of each object in an Iterator to a file. Each object is written as a single line using the default charset.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  the Iterator containing the objects to be written.
     * @param output the File where the objects' string representations are to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
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
     * Writes the string representation of each object in an Iterator to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  the Iterator containing the objects to be written.
     * @param output the Writer where the objects' string representations are to be written, must not be {@code null}
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
     * @param lines  the Iterator containing the objects to be written.
     * @param output the Writer where the objects' string representations are to be written, must not be {@code null}
     * @param flush  if {@code true}, the stream will be flushed after writing the lines.
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

                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
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
     * @param lines  the Iterable containing the objects to be written.
     * @param output the File where the objects' string representations are to be written, must not be {@code null} 
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
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

    /**
     * Writes the string representation of each object in an Iterable to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * @param lines  the Iterable containing the objects to be written.
     * @param output the Writer where the objects' string representations are to be written, must not be {@code null}
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
     * @param lines  the Iterable containing the objects to be written.
     * @param output the Writer where the objects' string representations are to be written, must not be {@code null}
     * @param flush  if {@code true}, the stream will be flushed after writing the lines.
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

                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
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
     * Writes the string representation of a boolean to a Writer.
     *
     * @param b      the boolean value to be written.
     * @param output the Writer where the boolean's string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final boolean b, final Writer output) throws IOException {
        output.write(N.stringOf(b));
    }

    /**
     * Writes a single character to a Writer.
     *
     * @param c      the character to be written.
     * @param output the Writer where the character is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final char c, final Writer output) throws IOException {
        output.write(c);
    }

    /**
     * Writes the string representation of a byte to a Writer.
     *
     * @param b      the byte value to be written.
     * @param output the Writer where the byte's string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte b, final Writer output) throws IOException {
        output.write(N.stringOf(b));
    }

    /**
     * Writes the string representation of a short to a Writer.
     *
     * @param s      the short value to be written.
     * @param output the Writer where the short's string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final short s, final Writer output) throws IOException {
        output.write(N.stringOf(s));
    }

    /**
     * Writes the string representation of an integer to a Writer.
     *
     * @param i      the integer value to be written.
     * @param output the Writer where the integer's string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final int i, final Writer output) throws IOException {
        output.write(N.stringOf(i));
    }

    /**
     * Writes the string representation of a long to a Writer.
     *
     * @param lng    the long value to be written.
     * @param output the Writer where the long's string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final long lng, final Writer output) throws IOException {
        output.write(N.stringOf(lng));
    }

    /**
     * Writes the string representation of a float to a Writer.
     *
     * @param f    the float value to be written.
     * @param output the Writer where the float string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final float f, final Writer output) throws IOException {
        output.write(N.stringOf(f));
    }

    /**
     * Writes the string representation of a double to a Writer.
     *
     * @param d    the double value to be written.
     * @param output the Writer where the double string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final double d, final Writer output) throws IOException {
        output.write(N.stringOf(d));
    }

    /**
     * Writes the string representation of an object to a Writer.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     * Note: This method also protects write(boolean/char/byte/.../double, Writer) from NullPointerException.
     *
     * @param obj    the object whose string representation is to be written.
     * @param output the Writer where the object's string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     * @see N#toString(Object)
     */
    public static void write(final Object obj, final Writer output) throws IOException { // Note: DO NOT remove/update this method because it also protects write(boolean/char/byte/.../double, Writer) from NullPointerException.
        output.write(N.toString(obj));
    }

    /**
     * Writes the byte array representation of a CharSequence to a File.
     *
     * @param cs     the CharSequence whose byte array representation is to be written.
     * @param output the File where the CharSequence's byte array representation is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes()
     */
    public static void write(final CharSequence cs, final File output) throws IOException {
        write(cs, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the byte array representation of a CharSequence to a File using the specified Charset.
     *
     * @param cs      the CharSequence whose byte array representation is to be written.
     * @param charset the Charset to be used to encode the CharSequence into a sequence of bytes.
     * @param output  the File where the CharSequence's byte array representation is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
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
     * @param cs     the CharSequence whose byte array representation is to be written.
     * @param output the OutputStream where the CharSequence's byte array representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes()
     */
    public static void write(final CharSequence cs, final OutputStream output) throws IOException {
        write(cs, output, false);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream using the specified Charset.
     *
     * @param cs      the CharSequence whose byte array representation is to be written.
     * @param charset the Charset to be used to encode the CharSequence into a sequence of bytes.
     * @param output  the File where the CharSequence's byte array representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes(Charset)
     */
    public static void write(final CharSequence cs, final Charset charset, final OutputStream output) throws IOException {
        write(cs, charset, output, false);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream.
     *
     * @param cs     the CharSequence whose byte array representation is to be written.
     * @param output the OutputStream where the CharSequence's byte array representation is to be written, must not be {@code null}
     * @param flush  if {@code true}, the output stream is flushed after writing the CharSequence.
     * @throws IOException if an I/O error occurs.
     * @see String#getBytes()
     */
    public static void write(final CharSequence cs, final OutputStream output, final boolean flush) throws IOException {
        write(cs, DEFAULT_CHARSET, output, flush);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream using the specified Charset.
     *
     * @param cs      the CharSequence whose byte array representation is to be written.
     * @param charset the Charset to be used to encode the CharSequence into a sequence of bytes.
     * @param output  the OutputStream where the CharSequence's byte array representation is to be written, must not be {@code null}
     * @param flush   if {@code true}, the output stream is flushed after writing the CharSequence.
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
     * @param cs     the CharSequence whose string representation is to be written.
     * @param output the Writer where the CharSequence's string representation is to be written, must not be {@code null}
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final CharSequence cs, final Writer output) throws IOException {
        write(cs, output, false);
    }

    /**
     * Writes the string representation of a CharSequence to a Writer.
     *
     * @param cs     the CharSequence whose string representation is to be written.
     * @param output the Writer where the CharSequence's string representation is to be written, must not be {@code null}
     * @param flush  if {@code true}, the Writer is flushed after writing the CharSequence.
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
     * @param chars  the character array whose byte array representation is to be written.
     * @param output the File where the character array's byte array representation is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void write(final char[] chars, final File output) throws IOException {
        if (N.isEmpty(chars)) {
            createNewFileIfNotExists(output);
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     * Writes the byte array representation of a character array to a File using the specified Charset.
     *
     * @param chars   the character array whose byte array representation is to be written.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes.
     * @param output  the File where the character array's byte array representation is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void write(final char[] chars, final Charset charset, final File output) throws IOException {
        if (N.isEmpty(chars)) {
            createNewFileIfNotExists(output);
            return;
        }

        write(chars, 0, chars.length, charset, output);
    }

    /**
     * Writes the byte array representation of a character array to a File.
     *
     * @param chars  the character array whose byte array representation is to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the File where the character array's byte array representation is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final File output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            createNewFileIfNotExists(output);
            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the byte array representation of a character array to a File using the specified Charset.
     *
     * @param chars   the character array whose byte array representation is to be written.
     * @param offset  the starting position in the character array.
     * @param count   the number of characters to be written from the character array.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes.
     * @param output  the File where the character array's byte array representation is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final File output) throws IOException {
        if (count == 0 && N.len(chars) >= offset) {
            createNewFileIfNotExists(output);
            return;
        }

        write(chars2Bytes(chars, offset, count, charset), output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream.
     *
     * @param chars  the character array whose byte array representation is to be written.
     * @param output the OutputStream where the character array's byte array representation is to be written.
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
     * @param chars   the character array whose byte array representation is to be written.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes.
     * @param output  the OutputStream where the character array's byte array representation is to be written.
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
     * @param chars  the character array whose byte array representation is to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the OutputStream where the character array's byte array representation is to be written, must not be {@code null}
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
     * @param chars   the character array whose byte array representation is to be written.
     * @param offset  the starting position in the character array.
     * @param count   the number of characters to be written from the character array.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes.
     * @param output  the OutputStream where the character array's byte array representation is to be written, must not be {@code null}
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
     * @param chars  the character array whose byte array representation is to be written.
     * @param output the OutputStream where the character array's byte array representation is to be written, must not be {@code null}
     * @param flush  if {@code true}, the output stream is flushed after writing the CharSequence.
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
     * @param chars  the character array whose byte array representation is to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the OutputStream where the character array's byte array representation is to be written, must not be {@code null}
     * @param flush  if {@code true}, the output stream is flushed after writing the CharSequence.
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
     * @param chars   the character array whose byte array representation is to be written.
     * @param offset  the starting position in the character array.
     * @param count   the number of characters to be written from the character array.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes.
     * @param output  the OutputStream where the character array's byte array representation is to be written, must not be {@code null}
     * @param flush   if {@code true}, the output stream is flushed after writing the CharSequence.
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
     * @param chars  the character array to be written.
     * @param output the Writer where the character array is to be written.
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
     * @param chars  the character array to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the Writer where the character array is to be written.
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
     * @param chars  the character array to be written.
     * @param output the Writer where the character array is to be written.
     * @param flush  if {@code true}, the output writer will be flushed after writing.
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
     * @param chars  the character array to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the Writer where the character array is to be written.
     * @param flush  if {@code true}, the output writer will be flushed after writing.
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
     * @param bytes  the byte array to be written.
     * @param output the File where the byte array is to be written.
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte[] bytes, final File output) throws IOException {
        if (N.isEmpty(bytes)) {
            createNewFileIfNotExists(output);
            return;
        }

        write(bytes, 0, bytes.length, output);
    }

    /**
     * Writes a portion of a byte array to a File.
     *
     * @param bytes  the byte array to be written.
     * @param offset the starting position in the byte array.
     * @param count  the number of bytes to be written from the byte array.
     * @param output the File where the byte array is to be written.
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     */
    public static void write(final byte[] bytes, final int offset, final int count, final File output) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(bytes) >= offset) {
            createNewFileIfNotExists(output);
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
     * @param bytes  the byte array to be written.
     * @param output the OutputStream where the byte array is to be written.
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
     * @param bytes  the byte array to be written.
     * @param offset the starting position in the byte array.
     * @param count  the number of bytes to be written from the byte array.
     * @param output the OutputStream where the byte array is to be written.
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
     * @param bytes  the byte array to be written.
     * @param output the OutputStream where the byte array is to be written.
     * @param flush  if {@code true}, the output stream is flushed after writing the byte array.
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
     * @param bytes  the byte array to be written.
     * @param offset the starting position in the byte array.
     * @param count  the number of bytes to be written from the byte array.
     * @param output the OutputStream where the byte array is to be written.
     * @param flush  if {@code true}, the output stream is flushed after writing the byte array.
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
     * @param source the file to read from.
     * @param output the file to write to.
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @return the total number of bytes written to the output file.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output);
    }

    /**
     * Writes a portion of a file to another file.
     *
     * @param source the source file to be written, must not be {@code null}
     * @param offset the starting position in the source file, in bytes.
     * @param count  the number of bytes to be written from the source file.
     * @param output the output file where the source file is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @return the total number of bytes written.
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
     * Writes the content of a source file to an {@code OutputStream}.
     *
     * @param source the source file to be written, must not be {@code null}
     * @param output the {@code OutputStream} where the source file is to be written, must not be {@code null}
     * @return the total number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final OutputStream output) throws IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of the source file to the output stream, starting from the specified offset and writing up to the specified count.
     *
     * @param source the file to read from.
     * @param offset the position in the file to start reading from.
     * @param count  the maximum number of bytes to write to the output.
     * @param output the output stream to write to.
     * @return the total number of bytes written to the output stream.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final long offset, final long count, final OutputStream output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of a source file to an {@code OutputStream}.
     *
     * @param source the source file to be written, must not be {@code null}
     * @param output the {@code OutputStream} where the source file is to be written, must not be {@code null}
     * @param flush  if {@code true}, the output stream will be flushed after the write operation.
     * @return the total number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final File source, final OutputStream output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes the content of the source file to the output stream, starting from the specified offset and writing up to the specified count.
     * if the flush parameter is {@code true}, the output stream is flushed after the write operation.
     *
     * @param source the file to read from.
     * @param offset the position in the file to start reading from.
     * @param count  the maximum number of bytes to write to the output.
     * @param output the output stream to write to.
     * @param flush  if {@code true}, the output stream is flushed after the write operation.
     * @return the total number of bytes written to the output stream.
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

    /**
     * Writes the content of the input stream to the specified output file.
     *
     * @param source the input stream to read from.
     * @param output the file to write to.
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @return the total number of bytes written to the output file.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output);
    }

    /**
     * Writes the content of an {@code InputStream} to a file.
     *
     * @param source the {@code InputStream} to be written, must not be {@code null}
     * @param offset the starting point from where to begin writing bytes from the {@code InputStream}, in bytes.
     * @param count  the maximum number of bytes to write to the file.
     * @param output the file where the {@code InputStream} is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @return the total number of bytes written.
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
     * Writes the content of an {@code InputStream} to an {@code OutputStream}.
     *
     * @param source the {@code InputStream} to be written, must not be {@code null}
     * @param output the {@code OutputStream} where the {@code InputStream} is to be written, must not be {@code null}
     * @return the total number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final OutputStream output) throws IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of an {@code InputStream} to an {@code OutputStream}.
     *
     * @param source the {@code InputStream} to be written, must not be {@code null}
     * @param offset the starting point from where to begin writing bytes from the {@code InputStream}, in bytes.
     * @param count  the maximum number of bytes to write to the {@code OutputStream}.
     * @param output the {@code OutputStream} where the {@code InputStream} is to be written, must not be {@code null}
     * @return the total number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of an {@code InputStream} to an {@code OutputStream}.
     *
     * @param source the {@code InputStream} to be written, must not be {@code null}
     * @param output the {@code OutputStream} where the {@code InputStream} is to be written, must not be {@code null}
     * @param flush  if {@code true}, the output stream will be flushed after writing.
     * @return the total number of bytes written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final OutputStream output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes the content of the input stream to the output stream, starting from the specified offset and writing up to the specified count.
     * if the flush parameter is {@code true}, the output stream is flushed after the write operation.
     *
     * @param source the input stream to read from.
     * @param offset the position in the input stream to start reading from.
     * @param count  the maximum number of bytes to write to the output.
     * @param output the output stream to write to.
     * @param flush  if {@code true}, the output stream is flushed after the write operation.
     * @return the total number of bytes written to the output stream.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);

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
     * Writes the content of a {@code Reader} to a file.
     *
     * @param source the {@code Reader} to be written, must not be {@code null}
     * @param output the file where the {@code Reader}'s content is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final File output) throws IOException {
        return write(source, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the content of a {@code Reader} to a file using the specified {@code Charset}.
     *
     * @param source  the {@code Reader} to be written, must not be {@code null}
     * @param charset the {@code Charset} to be used to open the specified file for writing. If {@code null}, the platform's default charset is used.
     * @param output  the file where the {@code Reader}'s content is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final Charset charset, final File output) throws IOException {
        return write(source, 0, Long.MAX_VALUE, charset, output);
    }

    /**
     * Writes the content of a {@code Reader} to a file starting from a specific offset and up to a certain count.
     *
     * @param source the {@code Reader} to be written, must not be {@code null}
     * @param offset the position in the {@code Reader} to start writing from, in characters.
     * @param count  the maximum number of characters to be written.
     * @param output the file where the {@code Reader}'s content is to be written, must not be {@code null}
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final long offset, final long count, final File output) throws IOException {
        return write(source, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the content of a {@code Reader} to a file starting from a specific offset and up to a certain count using the specified {@code Charset}.
     *
     * @param source  the {@code Reader} to be written, must not be {@code null}
     * @param offset  the position in the {@code Reader} to start writing from, in characters.
     * @param count   the maximum number of characters to be written.
     * @param charset the {@code Charset} to be used to open the specified file for writing. If {@code null}, the platform's default charset is used.
     *      if the file exists, it will be overwritten. if the file's parent directory doesn't exist, it will be created.
     * @param output  the file where the {@code Reader}'s content is to be written, must not be {@code null}
     * @return the total number of characters written.
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
     * Writes the content from a {@code Reader} to a {@code Writer}.
     *
     * @param source the {@code Reader} to be written, must not be {@code null}
     * @param output the {@code Writer} where the {@code Reader}'s content is to be written, must not be {@code null}
     * @return the total number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final Writer output) throws IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of a {@code Reader} to a {@code Writer} starting from a specific offset and up to a certain count.
     *
     * @param source the {@code Reader} to be written, must not be {@code null}
     * @param offset the position in the {@code Reader} to start writing from, in characters.
     * @param count  the maximum number of characters to be written.
     * @param output the {@code Writer} where the {@code Reader}'s content is to be written, must not be {@code null}
     * @return the total number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final long offset, final long count, final Writer output) throws IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of a {@code Reader} to a {@code Writer}.
     *
     * @param source the {@code Reader} to be written, must not be {@code null}
     * @param output the {@code Writer} where the {@code Reader}'s content is to be written, must not be {@code null}
     * @param flush  if {@code true}, the output {@code Writer} is flushed after writing.
     * @return the total number of characters written.
     * @throws IOException if an I/O error occurs.
     */
    public static long write(final Reader source, final Writer output, final boolean flush) throws IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes a specified number of characters from a Reader to a Writer, starting from a specified offset.
     *
     * @param source the Reader to read from.
     * @param offset the position in the Reader to start reading from.
     * @param count  the number of characters to read from the Reader and write to the Writer.
     * @param output the Writer to write to.
     * @param flush  if {@code true}, the output Writer is flushed after writing.
     * @return the total number of characters written to the Writer.
     * @throws IllegalArgumentException if the source Reader is {@code null} or if the count is negative.
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
     * @param bytes      the byte array to append to the file.
     * @param targetFile the file to which the byte array will be appended.
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
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
     * @param bytes      the byte array to append to the file.
     * @param offset     the starting index from where to append the bytes.
     * @param count      the number of bytes to append from the byte array.
     * @param targetFile the file to which the byte array will be appended.
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
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
     * @param chars      the character array to append to the file.
     * @param targetFile the file to which the character array will be appended.
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void append(final char[] chars, final File targetFile) throws IOException {
        if (N.isEmpty(chars)) {
            createNewFileIfNotExists(targetFile);
            return;
        }

        append(chars, 0, chars.length, targetFile);
    }

    /**
     * Appends the given character array to the specified file.
     *
     * @param chars      the character array to append to the file.
     * @param charset    the Charset to be used to encode the character array into a sequence of bytes.
     * @param targetFile the file to which the character array will be appended.
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], Charset)
     */
    public static void append(final char[] chars, final Charset charset, final File targetFile) throws IOException {
        if (N.isEmpty(chars)) {
            createNewFileIfNotExists(targetFile);
            return;
        }

        append(chars, 0, chars.length, charset, targetFile);
    }

    /**
     * Appends the content of the character array to the target file.
     * The content to be appended starts at the specified offset and extends count characters.
     *
     * @param chars      the character array to append to the file.
     * @param offset     the initial offset in the character array.
     * @param count      the number of characters to append.
     * @param targetFile the file to which the character array will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void append(final char[] chars, final int offset, final int count, final File targetFile) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            createNewFileIfNotExists(targetFile);
            return;
        }

        append(chars, offset, count, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of the character array to the target file.
     * The content to be appended starts at the specified offset and extends count characters.
     *
     * @param chars      the character array to append to the file.
     * @param offset     the initial offset in the character array.
     * @param count      the number of characters to append.
     * @param charset    the Charset to be used to encode the character array into a sequence of bytes.
     * @param targetFile the file to which the character array will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #chars2Bytes(char[], int, int, Charset)
     */
    public static void append(final char[] chars, final int offset, final int count, final Charset charset, final File targetFile) throws IOException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            createNewFileIfNotExists(targetFile);
            return;
        }

        append(chars2Bytes(chars, offset, count, charset), targetFile);
    }

    /**
     * Appends the content of the CharSequence to the target file.
     *
     * @param cs         the CharSequence to append to the file.
     * @param targetFile the file to which the CharSequence will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     */
    public static void append(final CharSequence cs, final File targetFile) throws IOException {
        append(cs, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of the CharSequence to the target file.
     *
     * @param cs         the CharSequence to append to the file.
     * @param charset    the Charset to be used to encode the character array into a sequence of bytes.
     * @param targetFile the file to which the CharSequence will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     */
    public static void append(final CharSequence cs, final Charset charset, final File targetFile) throws IOException {
        append(toByteArray(cs, charset), targetFile);
    }

    /**
     * Appends the content of the source file to the target file.
     *
     * @param source     the source file to read from, must not be {@code null}
     * @param targetFile the target file to append to, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @return the number of bytes appended.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final File source, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, targetFile);
    }

    /**
     * Appends the content of the source file to the target file.
     * The content to be appended is read from the source file starting from the specified offset in bytes and up to the specified count.
     *
     * @param source     the source file to read from, must not be {@code null}
     * @param offset     the starting point in bytes from where to read in the source file.
     * @param count      the maximum number of bytes to read from the source file.
     * @param targetFile the file to which the content will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @return the number of bytes appended to the target file.
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
     * @param source     the InputStream to read from, must not be {@code null}
     * @param targetFile the file to which the InputStream content will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @return the number of bytes appended to the target file.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final InputStream source, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, targetFile);
    }

    /**
     * Appends the content of the InputStream to the target file.
     * The content to be appended is read from the InputStream starting from the specified offset in bytes and up to the specified count.
     *
     * @param source     the InputStream to read from, must not be {@code null}
     * @param offset     the starting point in bytes from where to read in the InputStream.
     * @param count      the maximum number of bytes to read from the InputStream.
     * @param targetFile the file to which the InputStream content will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @return the number of bytes appended to the target file.
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
     * @param source     the Reader to read from, must not be {@code null}
     * @param targetFile the file to which the Reader content will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @return the number of characters appended to the target file.
     * @throws IOException if an I/O error occurs.
     */

    public static long append(final Reader source, final File targetFile) throws IOException {
        return append(source, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of a {@code Reader} to a file using the specified character set.
     * The input reader is not closed by this method.
     *
     * @param source the {@code Reader} to read from, must not be {@code null}
     * @param charset the character set to use for encoding, if {@code null} the platform's default charset is used
     * @param targetFile the file where the {@code Reader}'s content is to be appended, must not be {@code null}.
     *                   If the file exists, content will be appended. If the file's parent directory doesn't exist, it will be created
     * @return the total number of characters appended
     * @throws IOException if an I/O error occurs
     */
    public static long append(final Reader source, final Charset charset, final File targetFile) throws IOException {
        return append(source, 0, Long.MAX_VALUE, charset, targetFile);
    }

    /**
     * Appends the content of the Reader to the target file.
     * The content to be appended is read from the Reader starting from the specified offset in characters and up to the specified count.
     * This file is opened to write with the default charset.
     *
     * @param source     the Reader to read from, must not be {@code null}
     * @param offset     the position in the Reader to start reading from.
     * @param count      the maximum number of characters to read from the Reader.
     * @param targetFile the file to which the Reader content will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @return the number of characters appended to the target file.
     * @throws IOException if an I/O error occurs.
     */
    public static long append(final Reader source, final long offset, final long count, final File targetFile) throws IOException {
        return append(source, offset, count, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of a {@code Reader} to a file using the specified character set, starting from a given offset and up to a specified count.
     * The input reader is not closed by this method.
     *
     * @param source the {@code Reader} to read from, must not be {@code null}
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0
     * @param count the maximum number of characters to read, must be &gt;= 0
     * @param charset the character set to use for encoding, if {@code null} the platform's default charset is used
     * @param targetFile the file where the {@code Reader}'s content is to be appended, must not be {@code null}.
     *                   If the file exists, content will be appended. If the file's parent directory doesn't exist, it will be created
     * @return the total number of characters appended
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative
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
     * @param obj        the object whose string representation is to be appended to the file.
     * @param targetFile the file to which the object's string representation will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
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
     * @param obj        the object whose string representation is to be appended to the file.
     * @param charset    the Charset to be used to encode string representation of the specified object into a sequence of bytes.
     * @param targetFile the file to which the object's string representation will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #writeLine(Object, File)
     * @see N#toString(Object)
     */
    public static void appendLine(final Object obj, final Charset charset, final File targetFile) throws IOException {
        final String str = N.toString(obj) + IOUtil.LINE_SEPARATOR_UNIX;

        append(toByteArray(str, charset), targetFile);
    }

    /**
     * Appends the string representation of each object in the provided iterable as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     * This file is opened to write with the default charset.
     *
     * @param lines      the iterable whose elements' string representations are to be appended to the file.
     * @param targetFile the file to which the elements' string representations will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #writeLines(Iterable, File)
     * @see N#toString(Object)
     */
    public static void appendLines(final Iterable<?> lines, final File targetFile) throws IOException {
        if (N.isEmpty(lines)) {
            createNewFileIfNotExists(targetFile);
            return;
        }

        appendLines(lines, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the string representation of each object in the provided iterable as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     * This file is opened to write with the provided charset.
     *
     * @param lines      the iterable whose elements' string representations are to be appended to the file.
     * @param charset    the Charset to be used to open the specified file for writing.
     * @param targetFile the file to which the elements' string representations will be appended, must not be {@code null}
     *      if the file exists, it will be appended. if the file's parent directory doesn't exist, it will be created.
     * @throws IOException if an I/O error occurs.
     * @see #writeLines(Iterable, File)
     * @see N#toString(Object)
     */
    public static void appendLines(final Iterable<?> lines, final Charset charset, final File targetFile) throws IOException {
        if (N.isEmpty(lines)) {
            createNewFileIfNotExists(targetFile);
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
     * @param src    the source channel from which bytes are to be read.
     * @param output the target channel to which bytes are to be written.
     * @return the number of bytes transferred.
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
     * @param input       the {@code InputStream} from which bytes are to be skipped, must not be {@code null}
     * @param bytesToSkip the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException if an I/O error occurs.
     */
    public static long skip(final InputStream input, final long bytesToSkip) throws IOException {
        if (bytesToSkip < 0) {
            throw new IllegalArgumentException("Skip count must be non-negative, actual: " + bytesToSkip);
        } else if (bytesToSkip == 0) {
            return 0;
        }

        final byte[] buf = Objectory.createByteArrayBuffer();
        long remain = bytesToSkip;

        try {
            while (remain > 0) {
                final long n = read(input, buf, 0, (int) Math.min(remain, buf.length));

                if (n < 0) { // EOF

                    break;
                }

                remain -= n;
            }

            return bytesToSkip - remain;
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Skips over and discards a specified number of characters from the input reader.
     *
     * @param input       the {@code Reader} from which characters are to be skipped, must not be {@code null}
     * @param charsToSkip the number of characters to be skipped.
     * @return the actual number of characters skipped.
     * @throws IOException if an I/O error occurs.
     */
    public static long skip(final Reader input, final long charsToSkip) throws IOException {
        if (charsToSkip < 0) {
            throw new IllegalArgumentException("Skip count must be non-negative, actual: " + charsToSkip);
        } else if (charsToSkip == 0) {
            return 0;
        }

        final char[] buf = Objectory.createCharArrayBuffer();
        long remain = charsToSkip;

        try {
            while (remain > 0) {
                final long n = read(input, buf, 0, (int) Math.min(remain, buf.length));

                if (n < 0) { // EOF

                    break;
                }

                remain -= n;
            }

            return charsToSkip - remain;
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Skips over and discards a specified number of bytes from the input stream.
     *
     * @param input       the input stream to be skipped, must not be {@code null}
     * @param bytesToSkip the number of bytes to be skipped.
     * @throws IOException if an I/O error occurs, including if the input stream reaches the end before skipping all the bytes.
     */
    public static void skipFully(final InputStream input, final long bytesToSkip) throws IOException {
        final long skipped = skip(input, bytesToSkip);

        if (skipped != bytesToSkip) {
            throw new IOException("Bytes to skip: " + bytesToSkip + ", actual: " + skipped);
        }
    }

    /**
     * Skips over and discards a specified number of characters from the input reader.
     *
     * @param input       the {@code Reader} from which characters are to be skipped, must not be {@code null}
     * @param charsToSkip the number of characters to be skipped.
     * @throws IOException if an I/O error occurs, including if the {@code Reader} reaches the end before skipping all the characters.
     */
    public static void skipFully(final Reader input, final long charsToSkip) throws IOException {
        final long skipped = skip(input, charsToSkip);

        if (skipped != charsToSkip) {
            throw new IOException("Chars to skip: " + charsToSkip + ", actual: " + skipped);
        }
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Maps a file into memory, creating a MappedByteBuffer that represents the file's content.
     *
     * <p><b>JVM Requirements:</b></p>
     * <ul>
     *   <li>Requires JVM flags: {@code --add-opens java.base/java.nio=ALL-UNNAMED}</li>
     *   <li>Requires: {@code --add-opens java.base/sun.nio.ch=ALL-UNNAMED}</li>
     *   <li>Without these flags, operations may fail with access errors</li>
     * </ul>
     *
     * @param file the file to be mapped into memory.
     * @return a MappedByteBuffer that represents the content of the file.
     * @throws IllegalArgumentException if the provided file is {@code null}.
     * @throws IOException              if an I/O error occurs during the operation.
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);

        return map(file, MapMode.READ_ONLY);
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Fully maps a file in to memory as per
     * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
     * using the requested {@link MapMode}.
     *
     * <p>Files are mapped from offset 0 to its length.
     *
     * <p>This only works for files {@code <= {@link Integer#MAX_VALUE}} bytes.
     *
     * <p><b>JVM Requirements:</b></p>
     * <ul>
     *   <li>Requires JVM flags: {@code --add-opens java.base/java.nio=ALL-UNNAMED}</li>
     *   <li>Requires: {@code --add-opens java.base/sun.nio.ch=ALL-UNNAMED}</li>
     *   <li>Without these flags, operations may fail with access errors</li>
     * </ul>
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file}
     * @return a buffer reflecting {@code file}
     * @throws IllegalArgumentException if the file or mode is {@code null}, or if the file does not exist.
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
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Maps a file in to memory as per
     * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)} using the requested {@link MapMode}.
     *
     * <p>Files are mapped from offset 0 to {@code size}.
     *
     * <p>If the mode is {@link MapMode#READ_WRITE} and the file does not exist,
     * it will be created with the requested {@code size}.
     * Thus, this method is useful for creating memory mapped files which do not yet exist.
     *
     * <p>This only works for files {@code <= {@link Integer#MAX_VALUE}} bytes.
     *
     * <p><b>JVM Requirements:</b></p>
     * <ul>
     *   <li>Requires JVM flags: {@code --add-opens java.base/java.nio=ALL-UNNAMED}</li>
     *   <li>Requires: {@code --add-opens java.base/sun.nio.ch=ALL-UNNAMED}</li>
     *   <li>Without these flags, operations may fail with access errors</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map a file for reading
     * File file = new File("data.bin");
     * MappedByteBuffer buffer = IOUtil.map(file, MapMode.READ_ONLY, 0, file.length());
     * byte value = buffer.get(0);
     *
     * // Map a portion of a large file for read-write
     * MappedByteBuffer rwBuffer = IOUtil.map(file, MapMode.READ_WRITE, 1024, 4096);
     * rwBuffer.putInt(0, 42);
     * }</pre>
     *
     * @param file   the file to map
     * @param mode   the mode to use when mapping {@code file}
     * @param offset the offset within the file at which the mapped region is to start; must be non-negative
     * @param count  the size of the region to be mapped; must be non-negative
     * @return a buffer reflecting {@code file}
     * @throws IllegalArgumentException if the preconditions on the parameters do not hold
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
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
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
     * @param pathname the file path to simplify.
     * @return the simplified path string with redundant elements removed.
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
        String result = Strings.join(path, "/");

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
     * Returns the file extension of the specified file. Empty string is returned if the file has no extension.
     *
     * @param file the file whose extension is to be retrieved.
     * @return the file extension, or {@code null} if the file is {@code null}.
     * @see FilenameUtil#getExtension(String) 
     */
    @MayReturnNull
    public static String getFileExtension(final File file) {
        if (file == null) {
            return null;
        }

        return FilenameUtil.getExtension(file.getName());
    }

    /**
     * Returns the file extension of the specified file name. Empty string is returned if the file has no extension.
     *
     * @param fileName the name of the file whose extension is to be retrieved.
     * @return the file extension, or {@code null} if the specified file name is {@code null}.
     * @see FilenameUtil#getExtension(String)
     */
    @MayReturnNull
    public static String getFileExtension(final String fileName) {
        return FilenameUtil.getExtension(fileName);
    }

    /**
     * Returns the file name without its extension.
     *
     * @param file the file whose name without extension is to be retrieved.
     * @return the file name without extension, or {@code null} if the file is {@code null}.
     * @see FilenameUtil#removeExtension(String)
     */
    @MayReturnNull
    public static String getNameWithoutExtension(final File file) {
        if (file == null) {
            return null;
        }

        return FilenameUtil.removeExtension(file.getName());
    }

    /**
     * Removes the file extension from a filename.
     * 
     * <p>This method removes the extension (the suffix starting from the last dot '.')
     * from the given filename. If no extension is found, the original filename is returned
     * unchanged.</p>
     * 
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.removeExtension("foo.txt");    // Returns "foo"
     * FilenameUtil.removeExtension("a\\b\\c.jpg"); // Returns "a\\b\\c"
     * FilenameUtil.removeExtension("a\\b\\c");     // Returns "a\\b\\c"
     * FilenameUtil.removeExtension("a.b\\c");      // Returns "a.b\\c"
     * FilenameUtil.removeExtension(null)            = null
     * }</pre>
     *
     * @param fileName the filename to query, {@code null} returns null
     * @return the filename minus the extension, or {@code null} if the input is {@code null}
     * @see FilenameUtil#removeExtension(String)
     */
    @MayReturnNull
    public static String getNameWithoutExtension(final String fileName) {
        return FilenameUtil.removeExtension(fileName);
    }

    /**
     * Creates a new AppendableWriter instance that wraps the provided Appendable object.
     *
     * @param appendable the Appendable object to be wrapped by the AppendableWriter, must not be {@code null}
     * @return a new instance of AppendableWriter that wraps the provided Appendable.
     * @throws IllegalArgumentException if the appendable is {@code null}.
     */
    public static AppendableWriter newAppendableWriter(final Appendable appendable) {
        return new AppendableWriter(appendable);
    }

    /**
     * Creates a new StringWriter instance.
     *
     * @return a new instance of StringWriter.
     */
    public static StringWriter newStringWriter() {
        return new StringWriter();
    }

    /**
     * Creates a new StringWriter instance with the specified initial size.
     *
     * @param initialSize the initial size of the buffer.
     * @return a new instance of StringWriter with the specified initial size.
     */
    public static StringWriter newStringWriter(final int initialSize) {
        return new StringWriter(initialSize);
    }

    /**
     * Creates a new StringWriter instance with the content of the specified StringBuilder.
     *
     * @param sb the StringBuilder whose content is to be used for the StringWriter.
     * @return a new instance of StringWriter with the content of the specified StringBuilder.
     */
    public static StringWriter newStringWriter(final StringBuilder sb) {
        return new StringWriter(sb);
    }

    /**
     * Creates a new ByteArrayOutputStream instance.
     *
     * @return a new instance of ByteArrayOutputStream.
     */
    public static ByteArrayOutputStream newByteArrayOutputStream() {
        return new ByteArrayOutputStream();
    }

    /**
     * Creates a new ByteArrayOutputStream instance with the specified initial capacity.
     *
     * @param initCapacity the initial capacity of the ByteArrayOutputStream.
     * @return a new instance of ByteArrayOutputStream with the specified initial capacity.
     */
    public static ByteArrayOutputStream newByteArrayOutputStream(final int initCapacity) {
        return new ByteArrayOutputStream(initCapacity);
    }

    /**
     * Creates a new FileInputStream instance for the specified file.
     *
     * @param file the file to be opened for reading.
     * @return a new FileInputStream instance.
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
     * @param name the name of the file to be opened for reading.
     * @return a new FileInputStream instance.
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
     * @param file the file to be opened for writing.
     * @return a new FileOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileOutputStream#FileOutputStream(File)
     */
    public static FileOutputStream newFileOutputStream(final File file) throws UncheckedIOException {
        try {
            createFileIfNotExists(file);

            return new FileOutputStream(file);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileOutputStream instance for the specified file.
     *
     * @param file   the file to be opened for writing.
     * @param append {@code true} if the file is to be opened for appending.
     * @return a new FileOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileOutputStream#FileOutputStream(File, boolean)
     */
    public static FileOutputStream newFileOutputStream(final File file, final boolean append) throws UncheckedIOException {
        try {
            createFileIfNotExists(file);

            return new FileOutputStream(file, append);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileOutputStream instance for the specified file name.
     *
     * @param name the name of the file to be opened for writing.
     * @return a new FileOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileOutputStream#FileOutputStream(String)
     */
    public static FileOutputStream newFileOutputStream(final String name) throws UncheckedIOException {
        return newFileOutputStream(new File(name));
    }

    /**
     * Creates a new FileReader instance for the specified file and the default Charset.
     *
     * @param file the file to be opened for reading.
     * @return a new FileReader instance.
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
     * @param file    the file to be opened for reading.
     * @param charset the Charset to be used for creating the FileReader.
     * @return a new FileReader instance.
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
     * @param file the file to be opened for writing.
     * @return a new FileWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileWriter#FileWriter(File, Charset)
     */
    public static FileWriter newFileWriter(final File file) throws UncheckedIOException {
        try {
            createNewFileIfNotExists(file);

            return new FileWriter(file, DEFAULT_CHARSET); // NOSONAR
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileWriter instance for the specified file and charset.
     *
     * @param file    the file to be opened for writing.
     * @param charset the Charset to be used for creating the FileWriter.
     * @return a new FileWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileWriter#FileWriter(File, Charset)
     */
    public static FileWriter newFileWriter(final File file, final Charset charset) throws UncheckedIOException {
        try {
            createNewFileIfNotExists(file);

            return new FileWriter(file, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileWriter instance for the specified file and charset.
     *
     * @param file    the file to be opened for writing.
     * @param charset the Charset to be used for creating the FileWriter.
     * @param append  {@code true} if the file is to be opened for appending.
     * @return a new FileWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see FileWriter#FileWriter(File, Charset, boolean)
     */
    public static FileWriter newFileWriter(final File file, final Charset charset, final boolean append) throws UncheckedIOException {
        try {
            createNewFileIfNotExists(file);

            return new FileWriter(file, charset, append);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new InputStreamReader instance for the specified InputStream and the default Charset.
     *
     * @param is the InputStream to be read.
     * @return a new InputStreamReader instance.
     * @see InputStreamReader#InputStreamReader(InputStream, Charset)
     */
    public static InputStreamReader newInputStreamReader(final InputStream is) {
        return new InputStreamReader(is, DEFAULT_CHARSET); // NOSONAR
    }

    /**
     * Creates a new InputStreamReader instance for the specified InputStream and Charset.
     *
     * @param is      the InputStream to be read.
     * @param charset the Charset to be used for creating the InputStreamReader.
     * @return a new InputStreamReader instance.
     * @see InputStreamReader#InputStreamReader(InputStream, Charset)
     */
    public static InputStreamReader newInputStreamReader(final InputStream is, final Charset charset) {
        return new InputStreamReader(is, checkCharset(charset)); // NOSONAR
    }

    /**
     * Creates a new OutputStreamWriter instance for the specified OutputStream and the default Charset.
     *
     * @param os the OutputStream to be written to.
     * @return a new OutputStreamWriter instance.
     * @see OutputStreamWriter#OutputStreamWriter(OutputStream, Charset)
     */
    public static OutputStreamWriter newOutputStreamWriter(final OutputStream os) {
        return new OutputStreamWriter(os, DEFAULT_CHARSET); // NOSONAR
    }

    /**
     * Creates a new OutputStreamWriter instance for the specified OutputStream and Charset.
     *
     * @param os      the OutputStream to be written to.
     * @param charset the Charset to be used for creating the OutputStreamWriter.
     * @return a new OutputStreamWriter instance.
     * @see OutputStreamWriter#OutputStreamWriter(OutputStream, Charset)
     */
    public static OutputStreamWriter newOutputStreamWriter(final OutputStream os, final Charset charset) {
        return new OutputStreamWriter(os, checkCharset(charset));
    }

    /**
     * Creates a new BufferedInputStream instance that wraps the specified InputStream.
     *
     * @param is the InputStream to be wrapped.
     * @return a new BufferedInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see BufferedInputStream#BufferedInputStream(InputStream)
     */
    public static BufferedInputStream newBufferedInputStream(final InputStream is) throws UncheckedIOException {
        return new BufferedInputStream(is);
    }

    /**
     * Creates a new BufferedReader instance for the specified file path.
     *
     * @param filePath the path of the file to be read.
     * @return a new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newBufferedReader(File)
     */
    static java.io.BufferedReader newBufferedReader(final String filePath) throws UncheckedIOException {
        return newBufferedReader(new File(filePath));
    }

    /**
     * Creates a new BufferedInputStream instance for the specified file.
     *
     * @param file the file to be read.
     * @return a new BufferedInputStream instance.
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
     * @param file the file to be read.
     * @param size the size of the buffer to be used.
     * @return a new BufferedInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newFileInputStream(File)
     * @see BufferedInputStream#BufferedInputStream(InputStream, int)
     */
    public static BufferedInputStream newBufferedInputStream(final File file, final int size) throws UncheckedIOException {
        return new BufferedInputStream(newFileInputStream(file), size);
    }

    /**
     * Creates a new BufferedOutputStream instance that wraps the specified OutputStream.
     *
     * @param os the OutputStream to be wrapped.
     * @return a new BufferedOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see BufferedOutputStream#BufferedOutputStream(OutputStream)
     */
    public static BufferedOutputStream newBufferedOutputStream(final OutputStream os) throws UncheckedIOException {
        return new BufferedOutputStream(os);
    }

    /**
     * Creates a new BufferedOutputStream instance for the specified file.
     *
     * @param file the file to be written to.
     * @return a new BufferedOutputStream instance.
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
     * @param file the file to be written to.
     * @param size the size of the buffer to be used.
     * @return a new BufferedOutputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newFileOutputStream(File)
     * @see BufferedOutputStream#BufferedOutputStream(OutputStream, int)
     */
    public static BufferedOutputStream newBufferedOutputStream(final File file, final int size) throws UncheckedIOException {
        return new BufferedOutputStream(newFileOutputStream(file), size);
    }

    /**
     * Creates a new BufferedReader instance that wraps the specified Reader.
     *
     * @param reader the Reader to be wrapped.
     * @return a new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final Reader reader) throws UncheckedIOException {
        return new java.io.BufferedReader(reader);
    }

    /**
     * Creates a new BufferedReader instance for the specified file with the default charset.
     *
     * @param file the file to be read from.
     * @return a new BufferedReader instance.
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
     * @param file    the file to be read from.
     * @param charset the charset to be used.
     * @return a new BufferedReader instance.
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
     * @param path the path of the file to be read from.
     * @return a new BufferedReader instance.
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
     * @param path    the path of the file to be read from.
     * @param charset the charset to be used.
     * @return a new BufferedReader instance.
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
     * @param is the InputStream to be read from.
     * @return a new BufferedReader instance.
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
     * @param is      the InputStream to be read from.
     * @param charset the charset to be used.
     * @return a new BufferedReader instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see #newInputStreamReader(InputStream, Charset)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final InputStream is, final Charset charset) throws UncheckedIOException {
        return new java.io.BufferedReader(newInputStreamReader(is, checkCharset(charset)));
    }

    /**
     * Creates a new BufferedWriter instance that wraps the specified Writer.
     *
     * @param writer the Writer to be wrapped.
     * @return a new BufferedWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final Writer writer) throws UncheckedIOException {
        return new java.io.BufferedWriter(writer);
    }

    /**
     * Creates a new BufferedWriter instance for the specified file path and default charset.
     *
     * @param filePath the path of the file to be written to.
     * @return a new BufferedWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    static java.io.BufferedWriter newBufferedWriter(final String filePath) throws UncheckedIOException {
        return newBufferedWriter(new File(filePath));
    }

    /**
     * Creates a new BufferedWriter instance for the specified file and default charset.
     *
     * @param file the file to be written to.
     * @return a new BufferedWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static java.io.BufferedWriter newBufferedWriter(final File file) throws UncheckedIOException {
        return new java.io.BufferedWriter(newFileWriter(file));
    }

    /**
     * Creates a new BufferedWriter instance for the specified file and charset.
     *
     * @param file    the file to be written to.
     * @param charset the charset to be used for writing to the file.
     * @return a new BufferedWriter instance.
     * @throws UncheckedIOException if an I/O error occurs.
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final File file, final Charset charset) throws UncheckedIOException {
        return new java.io.BufferedWriter(newFileWriter(file, checkCharset(charset)));
    }

    /**
     * Creates a new BufferedWriter instance for the specified OutputStream with default charset.
     *
     * @param os the OutputStream to be written to.
     * @return a new BufferedWriter instance.
     * @see #newOutputStreamWriter(OutputStream)
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final OutputStream os) {
        return new java.io.BufferedWriter(newOutputStreamWriter(os)); // NOSONAR
    }

    /**
     * Creates a new BufferedWriter instance for the specified OutputStream and Charset.
     *
     * @param os      the OutputStream to be written to.
     * @param charset the Charset to be used for writing to the OutputStream.
     * @return a new BufferedWriter instance.
     * @see #newOutputStreamWriter(OutputStream, Charset)
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final OutputStream os, final Charset charset) {
        return new java.io.BufferedWriter(newOutputStreamWriter(os, checkCharset(charset)));
    }

    /**
     * Creates a new LZ4BlockInputStream instance for the specified InputStream.
     *
     * @param is the InputStream to be read from.
     * @return a new LZ4BlockInputStream instance.
     */
    public static LZ4BlockInputStream newLZ4BlockInputStream(final InputStream is) {
        return new LZ4BlockInputStream(is);
    }

    /**
     * Creates a new LZ4BlockOutputStream instance for the specified OutputStream.
     *
     * @param os the OutputStream to be written to.
     * @return a new LZ4BlockOutputStream instance.
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os) {
        return new LZ4BlockOutputStream(os);
    }

    /**
     * Creates a new LZ4BlockOutputStream instance for the specified OutputStream with the given block size.
     *
     * @param os        the OutputStream to be written to.
     * @param blockSize the block size for the LZ4BlockOutputStream.
     * @return a new LZ4BlockOutputStream instance.
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os, final int blockSize) {
        return new LZ4BlockOutputStream(os, blockSize);
    }

    /**
     * Creates a new SnappyInputStream instance for the specified InputStream.
     *
     * @param is the InputStream to be read from.
     * @return a new SnappyInputStream instance.
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
     * @param os the OutputStream to be written to.
     * @return a new SnappyOutputStream instance.
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os) {
        return new SnappyOutputStream(os);
    }

    /**
     * Creates a new SnappyOutputStream instance with the specified OutputStream and buffer size.
     *
     * @param os         the OutputStream to be written to.
     * @param bufferSize the size of the buffer to be used.
     * @return a new SnappyOutputStream instance.
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os, final int bufferSize) {
        return new SnappyOutputStream(os, bufferSize);
    }

    /**
     * Creates a new GZIPInputStream instance for the specified InputStream.
     *
     * @param is the InputStream to be read from.
     * @return a new GZIPInputStream instance.
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
     * @param is         the InputStream to be read from.
     * @param bufferSize the size of the buffer to be used.
     * @return a new GZIPInputStream instance.
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
     * @param os the OutputStream to be written to.
     * @return a new GZIPOutputStream instance.
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
     * @param os         the OutputStream to be written to.
     * @param bufferSize the size of the buffer to be used.
     * @return a new GZIPOutputStream instance.
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
     * @param is the InputStream to be used for creating the ZipInputStream.
     * @return a new ZipInputStream instance.
     * @see ZipInputStream#ZipInputStream(InputStream)
     */
    public static ZipInputStream newZipInputStream(final InputStream is) {
        return new ZipInputStream(is);
    }

    /**
     * Creates a new ZipInputStream with the specified InputStream and Charset.
     *
     * @param is      the InputStream to be used for creating the ZipInputStream.
     * @param charset the Charset to be used for creating the ZipInputStream.
     * @return a new ZipInputStream instance.
     * @see ZipInputStream#ZipInputStream(InputStream, Charset)
     */
    public static ZipInputStream newZipInputStream(final InputStream is, final Charset charset) {
        return new ZipInputStream(is, checkCharset(charset));
    }

    /**
     * Creates a new ZipOutputStream with the specified OutputStream.
     *
     * @param os the OutputStream to be used for creating the ZipOutputStream.
     * @return a new ZipOutputStream instance.
     * @see ZipOutputStream#ZipOutputStream(OutputStream)
     */
    public static ZipOutputStream newZipOutputStream(final OutputStream os) {
        return new ZipOutputStream(os);
    }

    /**
     * Creates a new ZipOutputStream with the specified OutputStream and Charset.
     *
     * @param os      the OutputStream to be used for creating the ZipOutputStream.
     * @param charset the Charset to be used for creating the ZipOutputStream.
     * @return a new ZipOutputStream instance.
     * @see ZipOutputStream#ZipOutputStream(OutputStream, Charset)
     */
    public static ZipOutputStream newZipOutputStream(final OutputStream os, final Charset charset) {
        return new ZipOutputStream(os, checkCharset(charset));
    }

    /**
     * Creates a new BrotliInputStream instance for the specified input stream.
     *
     * @param is the input stream to be used for creating the BrotliInputStream.
     * @return a new BrotliInputStream instance.
     * @throws UncheckedIOException if an I/O error occurs.
     */
    public static BrotliInputStream newBrotliInputStream(final InputStream is) throws UncheckedIOException {
        try {
            return new BrotliInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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
     * if the object is {@code null}, this method does nothing.
     *
     * @param closeable the AutoCloseable object to be closed. It can be {@code null}.
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
     * @param closeable        the AutoCloseable object to be closed, may be {@code null}.
     * @param exceptionHandler the Consumer to handle any exceptions thrown during the close operation, must not be {@code null}
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
     * @param a the AutoCloseable objects to be closed. It may contain {@code null} elements.
     */
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
     * @param c the Iterable of AutoCloseable objects to be closed. It may contain {@code null} elements.
     */
    public static void closeAll(final Iterable<? extends AutoCloseable> c) {
        if (N.isEmpty(c)) {
            return;
        }

        Exception ex = null;

        for (final AutoCloseable closeable : c) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
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
     * @param closeable the AutoCloseable object to be closed. It can be {@code null}.
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
     * @param a the AutoCloseable objects to be closed. It may contain {@code null} elements.
     */
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
     * @param c the Iterable of AutoCloseable objects to be closed. It may contain {@code null} elements.
     */
    public static void closeAllQuietly(final Iterable<? extends AutoCloseable> c) {
        if (N.isEmpty(c)) {
            return;
        }

        for (final AutoCloseable closeable : c) {
            closeQuietly(closeable);
        }
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     *
     * @param srcFile the source file or directory to be copied. It must not be {@code null}.
     * @param destDir the destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @throws IOException if an I/O error occurs.
     */
    public static void copyToDirectory(final File srcFile, final File destDir) throws IOException {
        copyToDirectory(srcFile, destDir, true);
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     * if the source is a directory, all its contents will be copied into the destination directory.
     *
     * @param srcFile          the source file or directory to be copied. It must not be {@code null}.
     * @param destDir          the destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @param preserveFileDate if {@code true}, the last modified date of the file will be preserved in the copied file.
     * @throws IOException if an I/O error occurs.
     */
    public static void copyToDirectory(final File srcFile, final File destDir, final boolean preserveFileDate) throws IOException {
        copyToDirectory(srcFile, destDir, preserveFileDate, Fn.BiPredicates.alwaysTrue());
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     * if the source is a directory, all its contents will be copied into the destination directory.
     *
     * @param <E>              the type of the exception that may be thrown by the filter.
     * @param srcFile          the source file or directory to be copied. It must not be {@code null}.
     * @param destDir          the destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @param preserveFileDate if {@code true}, the last modified date of the file will be preserved in the copied file.
     * @param filter           a BiPredicate that takes the source and destination files as arguments and returns a boolean. if the predicate returns {@code true}, the file is copied; if it returns {@code false}, the file is not copied.
     * @throws IOException if an I/O error occurs.
     * @throws E                    if the filter throws an exception.
     */
    public static <E extends Exception> void copyToDirectory(File srcFile, File destDir, final boolean preserveFileDate,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws IOException, E {
        N.checkArgNotNull(filter, cs.filter);

        checkFileExists(srcFile, true);
        checkDestDirectory(destDir);

        srcFile = srcFile.getCanonicalFile();
        destDir = destDir.getCanonicalFile();

        String srcCanonicalPath = srcFile.getCanonicalPath();
        String destCanonicalPath = destDir.getCanonicalPath();

        if (srcFile.isDirectory()) {
            if (destCanonicalPath.startsWith(srcCanonicalPath) && (destCanonicalPath.length() == srcCanonicalPath.length()
                    || destCanonicalPath.charAt(srcCanonicalPath.length()) == '/' || destCanonicalPath.charAt(srcCanonicalPath.length()) == '\\')) {
                throw new IllegalArgumentException(
                        "Failed to copy due to the target directory: " + destCanonicalPath + " is in or same as the source directory: " + srcCanonicalPath);
            }

            final File targetDir = new File(destDir, srcFile.getName());

            doCopyDirectory(srcFile, targetDir, preserveFileDate, filter);
        } else {
            File destFile = null;

            if (destDir.getCanonicalPath().equals(srcFile.getParentFile().getCanonicalPath())) {
                destFile = new File(destDir, "Copy of " + srcFile.getName());
            } else {
                destFile = new File(destDir, srcFile.getName());
            }

            doCopyFile(srcFile, destFile, preserveFileDate);
        }
    }

    /**
     * Internal copy directory method.
     *
     * @param <E>              the type of exception that the filter may throw during file filtering.
     * @param srcDir           the validated source directory, must not be {@code null}
     * @param destDir          the validated destination directory, must not be {@code null}
     * @param preserveFileDate whether to preserve the file date.
     * @param filter           the filter to apply, {@code null} means copy all directories and files.
     * @throws IOException if an I/O error occurs.
     * @throws E           if filter throws an exception during file filtering.
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static <E extends Exception> void doCopyDirectory(final File srcDir, final File destDir, final boolean preserveFileDate,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws IOException, E {
        N.checkArgNotNull(filter, cs.filter);

        checkDestDirectory(destDir);

        final File[] subFiles = srcDir.listFiles();

        if (N.isEmpty(subFiles)) {
            return;
        }

        for (final File subFile : subFiles) {
            if (subFile == null) {
                continue;
            }

            if (filter.test(srcDir, subFile)) {
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
            setTimes(srcDir, destDir);
        }
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static void doCopyFile(final File srcFile, final File destFile, final boolean preserveFileDate) throws IOException {
        if (destFile.exists()) {
            throw new IOException("The destination file already existed: " + destFile.getAbsolutePath());
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
        } finally {
            close(output);
            close(fos);
            close(input);
            close(fis);
        }

        if (srcFile.length() != destFile.length()) {
            deleteAllIfExists(destFile);
            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
        }

        if (preserveFileDate && !Files.isSymbolicLink(srcFile.toPath()) && !setTimes(srcFile, destFile)) {
            throw new IOException("Cannot set the file time.");
        }
    }

    /**
     * Throws IllegalArgumentException if the given files' canonical representations are equal.
     *
     * @param file1 the first file to compare.
     * @param file2 the second file to compare.
     * @throws IOException if an I/O error occurs.
     * @throws IllegalArgumentException if the given files' canonical representations are equal.
     */
    private static void requireCanonicalPathsNotEquals(final File file1, final File file2) throws IOException {
        final String canonicalPath = file1.getCanonicalPath();
        if (canonicalPath.equals(file2.getCanonicalPath())) {
            throw new IllegalArgumentException(String.format("File canonical paths are equal: '%s' (file1='%s', file2='%s')", canonicalPath, file1, file2));
        }
    }

    /**
     * Creates all parent directories for a File object, including any necessary but non-existent parent directories. If a parent directory already exists or
     * is {@code null}, nothing happens.
     *
     * @param file the File that may need parents, which may be {@code null}.
     * @return the parent directory, or {@code null} if the given File does have a parent.
     * @since 2.9.0
     */
    private static boolean createParentDirectories(final File file) {
        return mkdirsIfNotExists(getParentFile(file));
    }

    /**
     * Gets the parent of the given file. The given file may be {@code null}. Note that a file's parent may be {@code null} as well.
     *
     * @param file the file to query, which may be {@code null}.
     * @return the parent file or {@code null}. Note that a file's parent may be {@code null} as well.
     */
    private static File getParentFile(final File file) {
        return file == null ? null : file.getParentFile();
    }

    /**
     * Sets file lastModifiedTime, lastAccessTime and creationTime to match the source file
     *
     * @param sourceFile the source file to query.
     * @param targetFile the target file or directory to set.
     * @return {@code true} if and only if the operation succeeded;
     *          {@code false} otherwise}.}.
     */
    private static boolean setTimes(final File sourceFile, final File targetFile) {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(targetFile, "targetFile");
        try {
            // Set creation, modified, last accessed to match source file
            final BasicFileAttributes srcAttr = Files.readAttributes(sourceFile.toPath(), BasicFileAttributes.class);
            final BasicFileAttributeView destAttrView = Files.getFileAttributeView(targetFile.toPath(), BasicFileAttributeView.class);
            // null guards are not needed; BasicFileAttributes.setTimes(...) is null safe
            destAttrView.setTimes(srcAttr.lastModifiedTime(), srcAttr.lastAccessTime(), srcAttr.creationTime());
            return true;
        } catch (final IOException ignored) {
            // Fallback: Only set modified time to match source file
            return targetFile.setLastModified(sourceFile.lastModified());
        }

        // TODO: (Help!) Determine historically why setLastModified(File, File) needed PathUtils.setLastModifiedTime() if
        //  sourceFile.isFile() was true, but needed setLastModifiedTime(File, long) if sourceFile.isFile() was false
    }

    /**
     * Copies the contents of a directory to another directory.
     * <p>
     * This method copies all files and subdirectories from the source directory to the destination directory.
     * if the destination directory does not exist, it is created.
     * </p>
     *
     * @param srcDir  the source directory to copy from, must not be {@code null}
     * @param destDir the destination directory to copy to, must not be {@code null}
     * @throws IOException          if an I/O error occurs or if the source directory does not exist.
     */
    public static void copyDirectory(final File srcDir, final File destDir) throws IOException {
        checkDirectoryExists(srcDir);
        checkDestDirectory(destDir);

        final List<File> files = listFiles(srcDir);

        if (N.notEmpty(files)) {
            for (final File file : files) {
                if (file != null) {
                    copyToDirectory(file, destDir);
                }
            }
        }
    }

    /**
     * Copies a file to a new location preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. if the destination file exists, then this method
     * overwrites it. A symbolic link is resolved before copying, so the new file is not a link.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the file's last modified date/times using
     * {@link BasicFileAttributeView#setTimes(FileTime, FileTime, FileTime)}. However, it is not guaranteed that the
     * operation will succeed. if the modification operation fails, it falls back to
     * {@link File#setLastModified(long)}, and if that fails, the method throws IOException.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}
     * @param destFile the new file, must not be {@code null}}.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeed.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes.
     * @see #copyToDirectory(File, File)
     * @see #copyFile(File, File, boolean)
     */
    public static void copyFile(final File srcFile, final File destFile) throws IOException {
        copyFile(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies an existing file to a new file location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. if the destination file exists, then this method
     * overwrites it. A symbolic link is resolved before copying so the new file is not a link.
     * </p>
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the file's last
     * modified date/times using {@link BasicFileAttributeView#setTimes(FileTime, FileTime, FileTime)}. However, it is
     * not guaranteed that the operation will succeed. if the modification operation fails, it falls back to
     * {@link File#setLastModified(long)}, and if that fails, the method throws IOException.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}
     * @param destFile the new file, must not be {@code null}
     * @param preserveFileDate {@code true} if the file date of the copy should be the same as the original.}.
     * @throws IOException if source or destination is invalid.
     * @throws IOException if an error occurs or setting the last-modified time didn't succeed.
     * @throws IOException if the output file length is not the same as the input file length after the copy completes
     * @see #copyFile(File, File, boolean, CopyOption...)
     */
    public static void copyFile(final File srcFile, final File destFile, final boolean preserveFileDate) throws IOException {
        copyFile(srcFile, destFile, preserveFileDate, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies a file to a new location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. if the destination file exists, you can overwrite
     * it if you use {@link StandardCopyOption#REPLACE_EXISTING}.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}
     * @param destFile the new file, must not be {@code null}
     * @param copyOptions options specifying how the copy should be done, for example {@link StandardCopyOption}.}.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IllegalArgumentException if source is not a file.
     * @throws IOException if an I/O error occurs.
     * @see StandardCopyOption
     * @since 2.9.0
     */
    public static void copyFile(final File srcFile, final File destFile, final CopyOption... copyOptions) throws IOException {
        copyFile(srcFile, destFile, true, copyOptions);
    }

    /**
     * Copies the contents of a file to a new location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. if the destination file exists, you can overwrite
     * it with {@link StandardCopyOption#REPLACE_EXISTING}.
     * </p>
     *
     * <p>
     * By default, a symbolic link is resolved before copying so the new file is not a link.
     * To copy symbolic links as links, you can pass {@code LinkOption.NO_FOLLOW_LINKS} as the last argument.
     * </p>
     *
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the file's last
     * modified date/times using {@link BasicFileAttributeView#setTimes(FileTime, FileTime, FileTime)}. However, it is
     * not guaranteed that the operation will succeed. if the modification operation fails, it falls back to
     * {@link File#setLastModified(long)}, and if that fails, the method throws IOException.
     * </p>
     *
     * @param srcFile an existing file to copy, must not be {@code null}
     * @param destFile the new file, must not be {@code null}
     * @param preserveFileDate {@code true} if the file date of the copy should be the same as the original.
     * @param copyOptions options specifying how the copy should be done, for example {@link StandardCopyOption}.}.
     * @throws FileNotFoundException if the source does not exist.
     * @throws IllegalArgumentException if {@code srcFile} or {@code destFile} is not a file
     * @throws IOException if the output file length is not the same as the input file length after the copy completes, or if an I/O error occurs, setting the last-modified time didn't succeed or the destination is not writable 
     * @see #copyToDirectory(File, File, boolean)
     * @since 2.8.0
     */
    public static void copyFile(final File srcFile, final File destFile, final boolean preserveFileDate, final CopyOption... copyOptions) throws IOException {
        Objects.requireNonNull(destFile, "destination");
        checkFileExists(srcFile);

        requireCanonicalPathsNotEquals(srcFile, destFile);
        createParentDirectories(destFile);

        if (destFile.exists()) {
            checkFileExists(destFile);
        }

        final Path srcPath = srcFile.toPath();

        Files.copy(srcPath, destFile.toPath(), copyOptions);

        // On Windows, the last modified time is copied by default.
        if (preserveFileDate && !Files.isSymbolicLink(srcPath) && !setTimes(srcFile, destFile)) {
            throw new IOException("Cannot set the file time.");
        }
    }

    /**
     * Copies bytes from a {@link File} to an {@link OutputStream}.
     * <p>
     * This method buffers the input internally, so there is no need to use a {@link BufferedInputStream}.
     * </p>
     *
     * @param input  the {@link File} to read.
     * @param output the {@link OutputStream} to write.
     * @return the number of bytes copied}.}.
     * @throws IOException          if an I/O error occurs.
     * @since 2.1
     */
    public static long copyFile(final File input, final OutputStream output) throws IOException {
        return copy(input.toPath(), output);
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
     * @throws IOException if an IO error occurs during copying
     */
    public static void copyURLToFile(final URL source, final File destination) throws IOException {
        InputStream is = null;
        try {
            is = source.openStream();

            write(is, destination);
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
     * @throws IOException if an IO error occurs during copying
     */
    public static void copyURLToFile(final URL source, final File destination, final int connectionTimeout, final int readTimeout) throws IOException {
        InputStream is = null;
        URLConnection connection = null;
        try {
            connection = source.openConnection();
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(readTimeout);
            is = connection.getInputStream();

            write(is, destination);
        } finally {
            close(is);
            close(connection);
        }
    }

    /**
     * Copies a file or directory from the source path to the target path.
     *
     * @param source  the source path of the file or directory to be copied.
     * @param target  the target path where the file or directory will be copied to.
     * @param options optional arguments that specify how the copy should be done.
     * @return the path to the target file or directory.
     * @throws IOException if an I/O error occurs.
     * @see Files#copy(Path, Path, CopyOption...)
     */
    public static Path copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        return Files.copy(source, target, options);
    }

    /**
     * Copies the content of the given InputStream to the specified target Path.
     *
     * @param in      the InputStream to be copied.
     * @param target  the target Path where the InputStream content will be copied to.
     * @param options optional arguments that specify how the copy should be done.
     * @return the number of bytes read or skipped and written to the target Path.
     * @throws IOException if an I/O error occurs during the copy operation
     * @see Files#copy(InputStream, Path, CopyOption...)
     */
    public static long copy(final InputStream in, final Path target, final CopyOption... options) throws IOException {
        return Files.copy(in, target, options);
    }

    /**
     * Copies the content of the file at the given source Path to the specified OutputStream.
     *
     * @param source the source Path of the file to be copied.
     * @param output the OutputStream where the file content will be copied to.
     * @return the number of bytes read or skipped and written to the OutputStream.
     * @throws IOException if an I/O error occurs during the copy operation.
     * @see Files#copy(Path, OutputStream)
     */
    public static long copy(final Path source, final OutputStream output) throws IOException {
        return Files.copy(source, output);
    }

    /**
     * <p>Moves a file from the source location to the destination directory, creating the destination directory if it doesn't exist.</p>
     *
     * <p>This method automatically uses {@code StandardCopyOption.REPLACE_EXISTING} as the copy option,
     * which means it will replace any existing file with the same name at the destination.
     * if the source is a directory, the entire directory and its contents will be moved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Move a file to another directory
     * File sourceFile = new File("/path/to/source/file.txt");
     * File targetDir = new File("/path/to/destination");
     * IOUtil.move(sourceFile, targetDir);
     * 
     * // The file is now at /path/to/destination/file.txt
     * // and has been removed from the original location
     * }</pre>
     *
     * @param srcFile the source file or directory to be moved
     * @param destDir the destination directory where the file or directory will be moved to
     * @throws IOException if an I/O error occurs during the move operation, such as if the source doesn't exist
     *         or the destination cannot be written to
     * @see #move(File, File, CopyOption...)
     * @see java.nio.file.Files#move(Path, Path, CopyOption...)
     */
    public static void move(final File srcFile, final File destDir) throws IOException {
        move(srcFile, destDir, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Moves a file from the source file to the target directory.
     * if the destination directory does not exist, it will be created.
     *
     * @param srcFile the source file to be moved.
     * @param destDir the target directory where the file will be moved to.
     * @param options optional arguments that specify how the move should be done.
     * @throws IOException if an I/O error occurs.
     */
    public static void move(final File srcFile, final File destDir, final CopyOption... options) throws IOException {
        if (!srcFile.exists()) {
            throw new IllegalArgumentException("The source file doesn't exist: " + srcFile.getAbsolutePath());
        }

        checkDestDirectory(destDir);

        move(srcFile.toPath(), destDir.toPath().resolve(srcFile.getName()), options);
    }

    /**
     * Moves a file from the source path to the target path.
     *
     * @param source  the source Path of the file to be moved.
     * @param target  the target Path where the file will be moved to.
     * @param options optional arguments that specify how the move should be done.
     * @return the target path.
     * @throws IOException if an I/O error occurs.
     * @see Files#move(Path, Path, CopyOption...)
     */
    public static Path move(final Path source, final Path target, final CopyOption... options) throws IOException {
        return Files.move(source, target, options);
    }

    /**
     * Renames the specified source file to the new file name provided.
     *
     * @param srcFile     the source file to be renamed.
     * @param newFileName the new name for the file.
     * @return {@code true} if the renaming succeeded, {@code false} otherwise.
     */
    public static boolean renameTo(final File srcFile, final String newFileName) {
        return srcFile.renameTo(new File(srcFile.getParent() + IOUtil.DIR_SEPARATOR + newFileName));
    }

    /**
     * Deletes the specified file if it exists by calling {@link File#delete()}.
     * <p>
     * This method attempts to delete the specified file or directory. if the file is {@code null}
     * or does not exist, the method returns {@code false} without attempting deletion.
     * For directories, this method only deletes empty directories - it will not delete
     * directories that contain files or subdirectories.
     * <p>
     * This is a simple deletion method that only removes the file itself. For recursive
     * deletion of directories and their contents, use {@link #deleteAllIfExists(File)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("./temp-file.txt");
     * boolean deleted = IOUtil.deleteIfExists(file);
     * if (deleted) {
     *     System.out.println("File deleted successfully.");
     * } else {
     *     System.out.println("File does not exist or could not be deleted.");
     * }
     * }</pre>
     *
     * @param file the file or directory to delete. Can be {@code null}.
     * @return {@code true} if the file was deleted successfully; {@code false} if the file
     *         is {@code null}, does not exist, or could not be deleted.
     * @see File#delete()
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     * @see #deleteAllIfExists(File)
     * @see #deleteQuietly(File)
     */
    public static boolean deleteIfExists(final File file) {
        if ((file == null) || !file.exists()) {
            return false;
        }

        return file.delete(); //NOSONAR
    }

    /**
     * Deletes the specified file or directory quietly, suppressing any exceptions that may occur.
     * <p>
     * This method attempts to delete the specified file or directory by calling {@link #deleteIfExists(File)}.
     * If any exception occurs during the deletion process, it is caught and logged, and the method
     * returns {@code false}. This makes it safe to use in situations where you want to attempt
     * deletion but don't want to handle exceptions explicitly.
     * <p>
     * Unlike {@link #deleteIfExists(File)}, this method will never throw an exception.
     * It only deletes the file itself, not its contents if it's a directory with files.
     * For recursive deletion, use {@link #deleteAllIfExists(File)} instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("./temp-file.txt");
     * boolean deleted = IOUtil.deleteQuietly(file);
     * // No exception handling needed - method handles all errors internally
     * System.out.println("File deleted: " + deleted);
     * }</pre>
     *
     * @param file the file or directory to delete. Can be {@code null}.
     * @return {@code true} if the file was deleted successfully; {@code false} if the file
     *         is {@code null}, does not exist, could not be deleted, or if an exception occurred.
     * @see File#delete()
     * @see Files#deleteIfExists(Path)
     * @see #deleteIfExists(File)
     * @see #deleteAllIfExists(File)
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
     * Deletes the specified file and all its subfiles/directories recursively if it's a directory.
     * <p>
     * This method performs a recursive deletion operation. if the specified file is a directory,
     * it will delete all files and subdirectories within it before deleting the directory itself.
     * if the file is a regular file, it will simply delete the file. if the file does not exist
     * or is {@code null}, the method returns {@code false} without performing any operations.
     * <p>
     * This operation is irreversible and will permanently remove all specified files and directories.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("./temp-folder");
     * boolean deleted = IOUtil.deleteAllIfExists(directory);
     * if (deleted) {
     *     System.out.println("File/directory and all contents deleted successfully.");
     * } else {
     *     System.out.println("File/directory does not exist or could not be deleted.");
     * }
     * }</pre>
     *
     * @param file the file or directory to delete recursively.
     * @return {@code true} if the file/directory and all its contents were deleted successfully;
     *         {@code false} if the file is {@code null}, does not exist, or could not be deleted.
     * @see File#delete()
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     * @see #deleteIfExists(File)
     * @see #deleteFilesFromDirectory(File)
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
     * Deletes all subfiles and subdirectories from the specified directory.
     * <p>
     * This method removes all files and directories contained within the specified directory,
     * but leaves the directory itself intact. if the directory does not exist or is actually
     * a file, the method returns {@code false} without performing any operations.
     * <p>
     * This operation is recursive - subdirectories and all their contents will be deleted.
     * The method is equivalent to calling {@code deleteFilesFromDirectory(dir, BiPredicates.alwaysTrue())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("./temp-folder");
     * boolean deleted = IOUtil.deleteFilesFromDirectory(directory);
     * if (deleted) {
     *     System.out.println("All files and subdirectories deleted successfully.");
     * } else {
     *     System.out.println("Failed to delete some files or subdirectories.");
     * }
     * }</pre>
     *
     * @param dir the directory from which to delete all files and subdirectories.
     * @return {@code true} if all files and directories were deleted successfully;
     *         {@code false} if the directory does not exist or is actually a file, 
     *         or some files could not be deleted or if the operation failed.
     * @see File#delete()
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     * @see #deleteFilesFromDirectory(File, Throwables.BiPredicate)
     * @see #deleteAllIfExists(File)
     */
    public static boolean deleteFilesFromDirectory(final File dir) {
        return deleteFilesFromDirectory(dir, BiPredicates.alwaysTrue());
    }

    /**
     * Deletes subfiles/directories from the specified directory based on the provided filter.
     * <p>
     * This method removes files and subdirectories within the specified directory that match
     * the given filter criteria. The directory itself is not deleted, only its contents.
     * <p>
     * The filter is a {@link Throwables.BiPredicate} that receives the parent directory and
     * each file/subdirectory as parameters. Only files/directories for which the filter
     * returns {@code true} will be deleted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("./temp-folder");
     * // Delete only .txt files
     * boolean deleted = IOUtil.deleteFilesFromDirectory(directory, 
     *     (parentDir, file) -> file.getName().endsWith(".txt"));
     * if (deleted) {
     *     System.out.println("Matching files deleted successfully.");
     * } else {
     *     System.out.println("Failed to delete some files.");
     * }
     * }</pre>
     *
     * @param <E> the type of exception that the filter may throw.
     * @param dir the directory from which to delete files and subdirectories.
     * @param filter the predicate to determine which files/directories should be deleted.
     *               Receives the parent directory and the file/directory being evaluated.
     * @return {@code true} if all matching files and directories were deleted successfully;
     *         {@code false} if the directory does not exist or is actually a file, 
     *         or some files could not be deleted or if the operation failed.
     * @throws E if the filter throws an exception during evaluation.
     * @see File#delete()
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     * @see #deleteFilesFromDirectory(File)
     * @see #deleteAllIfExists(File)
     */
    public static <E extends Exception> boolean deleteFilesFromDirectory(final File dir, final Throwables.BiPredicate<? super File, ? super File, E> filter)
            throws E {
        N.checkArgNotNull(filter, cs.filter);

        if ((dir == null) || !dir.exists() || dir.isFile()) {
            return false;
        }

        final File[] files = dir.listFiles();

        if (N.isEmpty(files)) {
            return true;
        }

        for (final File subFile : files) {
            if (subFile == null) {
                continue;
            }

            if (filter.test(dir, subFile)) {
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
                if (subFile.isDirectory() && !deleteFilesFromDirectory(subFile, filter)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Cleans a directory by deleting all subfiles and subdirectories within it.
     * <p>
     * This method removes all files and directories contained within the specified directory,
     * but leaves the directory itself intact. if the directory does not exist or is actually
     * a file, the method returns {@code false} without performing any operations.
     * <p>
     * This operation is recursive - subdirectories and all their contents will be deleted.
     * The method is equivalent to calling {@code deleteFilesFromDirectory(directory)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("./temp-folder");
     * boolean cleaned = IOUtil.cleanDirectory(directory);
     * if (cleaned) {
     *     System.out.println("Directory cleaned successfully.");
     * } else {
     *     System.out.println("Failed to clean some files in the directory.");
     * }
     * }</pre>
     *
     * @param dir the directory to clean.
     * @return {@code true} if all subfiles and subdirectories were deleted successfully; 
     *         {@code false} if the directory does not exist or is actually a file, 
     *         or some files could not be deleted or if the operation failed.
     * @see #deleteFilesFromDirectory(File)
     * @see #deleteAllIfExists(File)
     * @see File#delete()
     */
    public static boolean cleanDirectory(final File dir) {
        return deleteFilesFromDirectory(dir, BiPredicates.alwaysTrue());
    }

    static boolean createNewFileIfNotExists(final File file) throws IOException {
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    // Too wide. It works with: File invalidFile = new File("/invalid/path/test.json");
                    // if (file.getParentFile().mkdirs()) {
                    if (file.getParentFile().mkdir()) {
                        return file.createNewFile();
                    }
                }
            } catch (final IOException e) {
                // if (!file.exists() && file.getParentFile().mkdirs()) {
                if (!file.exists() && file.getParentFile().mkdir()) {
                    return file.createNewFile();
                }

                throw e;
            }

            return true;
        }

        return false;
    }

    /**
     * Creates a new empty file if one doesn't already exist at the specified path.
     * <p>
     * This method attempts to create a new file at the path specified by the input File
     * object, but only if a file or directory doesn't already exist at that location.
     * if the parent directory doesn't exist, the method will attempt to create it before
     * creating the file.
     * 
     * <p>
     * Example usage:
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File configFile = new File("/path/to/config.json");
     * boolean created = IOUtil.createFileIfNotExists(configFile);
     * if (created) {
     *     System.out.println("Created new config file");
     * } else {
     *     System.out.println("Config file already exists");
     * }
     * }</pre>
     *
     * @param file the File object representing the file to create
     * @return {@code true} if a new file was created successfully; {@code false} if the file already exists
     * @throws UncheckedIOException if an I/O error occurs during file creation
     * @see File#createNewFile()
     * @see #mkdirIfNotExists(File)
     * @see #mkdirsIfNotExists(File)
     */
    public static boolean createFileIfNotExists(final File file) throws UncheckedIOException {
        try {
            return createNewFileIfNotExists(file);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to create file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Creates a directory if it does not already exist.
     * <p>
     * This method attempts to create the directory named by the given {@code File} object.
     * If a directory with this name already exists, the method returns {@code false}.
     * if the directory does not exist, it is created. This method does not create parent
     * directories. Use {@link #mkdirsIfNotExists(File)} to create parent directories as well.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File newDir = new File("./new-directory");
     * boolean result = IOUtil.mkdirIfNotExists(newDir);
     * if (result) {
     *     System.out.println("Directory exists or was created successfully.");
     * } else {
     *     System.out.println("Failed to create directory or a file with the same name exists.");
     * }
     * }</pre>
     *
     * @param dir the directory to create.
     * @return {@code true} if the directory was created successfully; {@code false} if the directory already exists or creation failed.
     * @see File#mkdir()
     * @see #mkdirsIfNotExists(File)
     * @see #createFileIfNotExists(File)
     */
    public static boolean mkdirIfNotExists(final File dir) {
        if (!(dir.exists() && dir.isDirectory())) {
            return dir.mkdir();
        }

        return false;
    }

    /**
     * Creates new directories if they do not exist.
     * <p>
     * This method attempts to create the directory named by the given {@code File} object,
     * including any necessary but nonexistent parent directories. If a directory with this 
     * name already exists, the method returns {@code false}. if the directory does not exist,
     * it and all necessary parent directories are created.
     * <p>
     * Unlike {@link #mkdirIfNotExists(File)}, this method will create parent directories
     * as needed to ensure the full directory path exists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File deepDir = new File("./parent/child/grandchild");
     * boolean created = IOUtil.mkdirsIfNotExists(deepDir);
     * if (created) {
     *     System.out.println("Directory hierarchy was created successfully.");
     * } else {
     *     System.out.println("Directory already exists or creation failed.");
     * }
     * }</pre>
     *
     * @param dir the directory to create, including any necessary parent directories.
     * @return {@code true} if the directories were created successfully; {@code false} if the directory already exists or creation failed.
     * @see File#mkdirs()
     * @see #mkdirIfNotExists(File)
     * @see #createFileIfNotExists(File)
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
     * @param reader the Reader to be checked.
     * @return {@code true} if the Reader is an instance of BufferedReader, {@code false} otherwise.
     */
    public static boolean isBufferedReader(final Reader reader) {
        return reader instanceof java.io.BufferedReader;
    }

    /**
     * Checks if the provided Writer is an instance of {@code java.io.BufferedWriter}.
     *
     * @param writer the Writer to be checked.
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
     * Checks if the specified {@code File} is a file or not.
     *
     * @param file the {@code File} to check
     * @return {@code true} if the {@code File} exists and is a file, {@code false} otherwise
     * @see File#isFile()
     */
    public static boolean isFile(final File file) {
        return file != null && file.isFile();
    }

    /**
     * Checks if the specified {@code File} is a directory or not.
     *
     * @param file the {@code File} to check
     * @return {@code true} if the {@code File} exists and is a directory, {@code false} otherwise
     * @see File#isDirectory()
     */
    public static boolean isDirectory(final File file) {
        return file != null && file.isDirectory();
    }

    /**
     * Tests whether the specified {@link File} is a directory or not. Implemented as a
     * null-safe delegate to {@link Files#isDirectory(Path path, LinkOption... options)}.
     *
     * @param   file the path to the file.
     * @param   options options indicating how symbolic links are handled
     * @return  {@code true} if the file is a directory; {@code false} if
     *          the path is {@code null}, the file does not exist, is not a directory, or it cannot
     *          be determined if the file is a directory or not.
     * @throws SecurityException     In the case of the default provider, and a security manager is installed, the
     *                               {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read
     *                               access to the directory.
     * @see Files#isDirectory(Path, LinkOption...)
     */
    public static boolean isDirectory(final File file, final LinkOption... options) {
        return file != null && Files.isDirectory(file.toPath(), options);
    }

    /**
     * Tests whether the specified {@link File} is a regular file or not. Implemented as a
     * null-safe delegate to {@link Files#isRegularFile(Path path, LinkOption... options)}.
     *
     * @param   file the path to the file.
     * @param   options options indicating how symbolic links are handled
     * @return  {@code true} if the file is a regular file; {@code false} if
     *          the path is {@code null}, the file does not exist, is not a regular file, or it cannot
     *          be determined if the file is a regular file or not.
     * @throws SecurityException     In the case of the default provider, and a security manager is installed, the
     *                               {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read
     *                               access to the directory.
     * @see Files#isRegularFile(Path, LinkOption...)
     */
    public static boolean isRegularFile(final File file, final LinkOption... options) {
        return file != null && Files.isRegularFile(file.toPath(), options);
    }

    /**
     * Checks if the specified file is a Symbolic Link rather than an actual file.
     *
     * @param file the file to be checked.
     * @return {@code true} if the file is a Symbolic Link, {@code false} otherwise.
     * @see Files#isSymbolicLink(Path)
     */
    public static boolean isSymbolicLink(final File file) {
        return file != null && Files.isSymbolicLink(file.toPath());
    }

    /**
     * Returns the size of the specified file or directory. if the provided
     * {@link File} is a regular file, then the file's length is returned.
     * if the argument is a directory, then the size of the directory is
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
     * @throws FileNotFoundException if the file is {@code null} or the file does not exist.
     * @see #sizeOfAsBigInteger(File)
     */
    public static long sizeOf(final File file) throws FileNotFoundException {
        return sizeOf(file, false);
    }

    /**
     * Returns the size of the specified file or directory.
     * @param file the file or directory whose size is to be calculated, must not be {@code null}
     * @param considerNonExistingFileAsEmpty if {@code true}, the size of non-existing file is considered as 0.
     * @return the total size in bytes. For directories, this is the recursive sum of all files within it.
     * @throws FileNotFoundException if the file does not exist and considerNonExistingFileAsEmpty is {@code false}.
     */
    public static long sizeOf(final File file, final boolean considerNonExistingFileAsEmpty) throws FileNotFoundException {
        if ((file == null || !file.exists()) && considerNonExistingFileAsEmpty) {
            return 0;
        }

        checkFileExists(file, true);

        if (file.isDirectory()) {
            return sizeOfDirectory0(file, considerNonExistingFileAsEmpty); // private method; expects directory
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
     * @throws FileNotFoundException if the directory is {@code null} or it's not existed directory.
     * @see #sizeOfDirectoryAsBigInteger(File)
     */
    public static long sizeOfDirectory(final File directory) throws FileNotFoundException {
        return sizeOfDirectory(directory, false);
    }

    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     *
     * @param directory the directory whose size is to be calculated, must not be {@code null}
     * @param considerNonExistingDirectoryAsEmpty if {@code true}, the size of non-existing directory is considered as 0.
     * @return the total size in bytes of all files within the directory and its subdirectories.
     * @throws FileNotFoundException if the directory does not exist and considerNonExistingDirectoryAsEmpty is {@code false}.
     */
    public static long sizeOfDirectory(final File directory, final boolean considerNonExistingDirectoryAsEmpty) throws FileNotFoundException {
        if ((directory == null || !directory.exists()) && considerNonExistingDirectoryAsEmpty) {
            return 0;
        }

        checkDirectoryExists(directory);

        return sizeOfDirectory0(directory, considerNonExistingDirectoryAsEmpty);
    }

    private static long sizeOf0(final File file, final boolean considerNonExistingFileAsEmpty) {
        if (file.isDirectory()) {
            return sizeOfDirectory0(file, considerNonExistingFileAsEmpty);
        }

        return file.length(); // will be 0 if file does not exist
    }

    private static long sizeOfDirectory0(final File directory, final boolean considerNonExistingDirectoryAsEmpty) {
        final File[] files = directory.listFiles();

        if (files == null) { // null if security restricted
            return 0L;
        }

        long size = 0;

        for (final File file : files) {
            if (!isSymbolicLink(file)) {
                size += sizeOf0(file, considerNonExistingDirectoryAsEmpty); // internal method
                if (size < 0) {
                    break;
                }
            }
        }

        return size;
    }

    /**
     * Returns the size of the specified file or directory as a BigInteger.
     *
     * @param file the file or directory whose size is to be calculated, must not be {@code null}
     * @return the total size as a BigInteger. For directories, this is the recursive sum of all files within it.
     * @throws FileNotFoundException if the file does not exist.
     * @see #sizeOf(File)
     * @see #sizeOfDirectoryAsBigInteger(File)
     */
    public static BigInteger sizeOfAsBigInteger(final File file) throws FileNotFoundException {
        checkFileExists(file, true);

        if (file.isDirectory()) {
            return sizeOfDirectoryAsBigInteger(file); // private method; expects directory
        }

        return BigInteger.valueOf(file.length());
    }

    /**
     * Returns the size of the specified directory as a BigInteger.
     *
     * @param directory the directory whose size is to be calculated, must not be {@code null}
     * @return the total size as a BigInteger of all files within the directory and its subdirectories.
     * @see #sizeOfDirectory(File)
     * @see #sizeOfAsBigInteger(File)
     */
    public static BigInteger sizeOfDirectoryAsBigInteger(final File directory) {
        N.checkArgNotNull(directory, cs.directory);

        return sizeOfDirectoryAsBigInteger0(directory);
    }

    private static BigInteger sizeOfAsBigInteger0(final File file) {
        if (file.isDirectory()) {
            return sizeOfDirectoryAsBigInteger0(file);
        }

        return BigInteger.valueOf(file.length()); // will be 0 if file does not exist
    }

    private static BigInteger sizeOfDirectoryAsBigInteger0(final File directory) {
        final File[] files = directory.listFiles();

        if (files == null) { // null if security restricted
            return BigInteger.ZERO;
        }

        BigInteger size = BigInteger.ZERO;

        for (final File file : files) {
            if (!isSymbolicLink(file)) {
                size = size.add(sizeOfAsBigInteger0(file)); // internal method 
            }
        }

        return size;
    }

    static File checkFileExists(final File file) throws FileNotFoundException {
        return checkFileExists(file, false);
    }

    static File checkFileExists(final File file, final boolean canBeDirectory) throws FileNotFoundException {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new FileNotFoundException("'" + file + "' does not exist or is not readable");
        }

        if (!file.isFile()) {
            if (file.isDirectory()) {
                if (!canBeDirectory) {
                    throw new IllegalArgumentException("'" + file.getAbsolutePath() + "' is not a file");
                }
            } else if (!Files.isSymbolicLink(file.toPath())) {
                throw new FileNotFoundException("'" + file + "' does not exist");
            }
        }

        return file;
    }

    static void checkDirectoryExists(final File directory) throws FileNotFoundException {
        if (directory == null || !directory.exists()) {
            throw new FileNotFoundException("'" + directory + "' does not exist");
        }

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("'" + directory.getAbsolutePath() + "' is not a directory");
        }
    }

    static void checkDestDirectory(final File destDir) throws IOException {
        if (destDir == null) {
            throw new IllegalArgumentException("The specified destination directory is null.");
        }

        if (destDir.exists()) {
            if (destDir.isFile()) {
                throw new IllegalArgumentException("Destination '" + destDir.getAbsolutePath() + "' is not a directory");
            }
        } else {
            if (!destDir.mkdirs()) {
                throw new IOException("Failed to create destination directory: " + destDir.getAbsolutePath());
            }
        }

        if (!destDir.canWrite()) {
            throw new IOException("Destination '" + destDir + "' cannot be written to"); //NOSONAR
        }
    }

    /**
     * Compresses the specified source file and writes the compressed data to the target file using the ZIP compression algorithm.
     *
     * @param sourceFile the file to be compressed. This must be a valid file.
     * @param targetFile the file to which the compressed data will be written. This must be a valid file.
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
     * @param sourceFiles the collection of files to be compressed. Each must be a valid file.
     * @param targetFile  the file to which the compressed data will be written. This must be a valid file.
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
     * @param sourceFile the file to be compressed. This must be a valid file.
     * @param zos        the ZipOutputStream to which the compressed data will be written.
     * @param targetFile the file to which the compressed data will be written. This must be a valid file.
     * @throws IOException if an I/O error occurs.
     */
    private static void zipFile(final File sourceFile, final ZipOutputStream zos, final File targetFile) throws IOException {
        if (sourceFile.isFile()) {
            zipFile(sourceFile, null, zos, targetFile);
        } else {
            Path sourcePath = sourceFile.toPath();
            Path parentPath = sourcePath.getParent();
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Relativize from parent to include source directory name
                    String zipEntryName = parentPath.relativize(file).toString();
                    zipEntryName = zipEntryName.replace('\\', '/');

                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    String zipEntryName = parentPath.relativize(dir) + "/";
                    zipEntryName = zipEntryName.replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(zipEntryName));
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Compresses the specified file and writes the compressed data to the target file using the provided ZipOutputStream.
     * This is a helper method used in the process of creating a ZIP file.
     *
     * @param file       the file to be compressed. This must be a valid file.
     * @param sourceDir  the directory containing the file to be compressed.
     * @param zos        the ZipOutputStream to which the compressed data will be written.
     * @param targetFile the file to which the compressed data will be written. This must be a valid file.
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
     * @param srcZipFile the source ZIP file to be unzipped. This must be a valid ZIP file.
     * @param targetDir  the directory to which the contents of the ZIP file will be extracted. This must be a valid directory.
     * @throws IOException if an I/O error occurs during the unzip process.
     */
    public static void unzip(final File srcZipFile, final File targetDir) throws IOException {
        checkFileExists(srcZipFile);
        checkDestDirectory(targetDir);

        ZipFile zip = null;
        ZipEntry ze = null;
        OutputStream os = null;
        InputStream is = null;

        final Path outputPath = targetDir.toPath();

        final byte[] buf = Objectory.createByteArrayBuffer();
        final int bufLength = buf.length;

        try { //NOSONAR
            zip = new ZipFile(srcZipFile);
            final Enumeration<? extends ZipEntry> entryEnum = zip.entries();

            while (entryEnum.hasMoreElements()) {
                ze = entryEnum.nextElement();

                if (ze.isDirectory()) {
                    Path entryPath = outputPath.resolve(ze.getName());
                    Files.createDirectories(entryPath);
                    continue;
                }

                os = IOUtil.newFileOutputStream(getAbsolutePath(targetDir, ze.getName()));

                try {
                    is = zip.getInputStream(ze);

                    int count = 0;

                    while (EOF != (count = read(is, buf, 0, bufLength))) {
                        os.write(buf, 0, count);
                    }

                    os.flush();
                } finally {
                    closeQuietly(is);
                    is = null;
                    close(os);
                    os = null;
                }
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
     * Splits a file into multiple parts based on the specified number of parts and saves them to the same directory as the source file.
     * <p>
     * This method divides the source file into a specified number of sequential parts. The parts are saved as
     * separate files in the same directory as the source file with numbered suffixes. This is useful for creating file chunks
     * for distribution, storage limitations, or transfer purposes.
     * </p>
     *
     * <p>File naming convention:</p>
     * <ul>
     *   <li><strong>Pattern:</strong> {@code originalFileName_NNNN}</li>
     *   <li><strong>Numbering:</strong> Sequential 4-digit zero-padded numbers starting from 0001</li>
     *   <li><strong>Example:</strong> {@code document.pdf} becomes {@code document.pdf_0001}, {@code document.pdf_0002}, etc.</li>
     * </ul>
     *
     * <p>Size distribution:</p>
     * <ul>
     *   <li><strong>Regular parts:</strong> Each part (except the last) will be approximately equal in size</li>
     *   <li><strong>Last part:</strong> Contains any remaining bytes, which may be smaller than the other parts</li>
     *   <li><strong>Empty files:</strong> if the source file is empty, one empty part file is created</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File sourceFile = new File("large_document.pdf");
     * 
     * // Split into 5 parts
     * IOUtil.split(sourceFile, 5);
     * // Creates: large_document.pdf_0001, large_document.pdf_0002, etc.
     * 
     * // Split into 10 parts
     * IOUtil.split(sourceFile, 10);
     * }</pre>
     *
     * @param file the source file to split; must exist and be readable
     * @param countOfParts the number of parts to split the file into; must be greater than 0
     * @throws IllegalArgumentException if {@code file} is {@code null} or {@code countOfParts} is less than 1
     * @throws FileNotFoundException if the source file does not exist
     * @throws IOException if there are issues with file validation or writing the parts
     * @see #split(File, int, File)
     * @see #splitBySize(File, long)
     * @see #splitBySize(File, long, File)
     */
    public static void split(final File file, final int countOfParts) throws IOException {
        split(file, countOfParts, file.getParentFile());
    }

    /**
     * Splits a file into multiple parts based on the specified number of parts and saves them to a destination directory.
     * <p>
     * This method divides the source file into a specified number of sequential parts. The parts are saved as
     * separate files in the destination directory with numbered suffixes. This is useful for creating file chunks
     * for distribution, storage limitations, or transfer purposes.
     * </p>
     *
     * <p>File naming convention:</p>
     * <ul>
     *   <li><strong>Pattern:</strong> {@code originalFileName_NNNN}</li>
     *   <li><strong>Numbering:</strong> Sequential 4-digit zero-padded numbers starting from 0001</li>
     *   <li><strong>Example:</strong> {@code document.pdf} becomes {@code document.pdf_0001}, {@code document.pdf_0002}, etc.</li>
     * </ul>
     *
     * <p>Size distribution:</p>
     * <ul>
     *   <li><strong>Regular parts:</strong> Each part (except the last) will be approximately equal in size</li>
     *   <li><strong>Last part:</strong> Contains any remaining bytes, which may be smaller than the other parts</li>
     *   <li><strong>Empty files:</strong> if the source file is empty, one empty part file is created</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File sourceFile = new File("large_document.pdf");
     * File outputDir = new File("split_parts");
     * 
     * // Split into 5 parts
     * IOUtil.split(sourceFile, 5, outputDir);
     * // Creates: large_document.pdf_0001, large_document.pdf_0002, etc.
     * 
     * // Split into 10 parts
     * IOUtil.split(sourceFile, 10, outputDir);
     * }</pre>
     *
     * @param file the source file to split; must exist and be readable
     * @param countOfParts the number of parts to split the file into; must be greater than 0
     * @param destDir the directory where the split parts will be saved; must exist and be writable
     * @throws IllegalArgumentException if {@code file} is {@code null} or {@code countOfParts} is less than 1
     * @throws FileNotFoundException if the source file does not exist
     * @throws IOException if there are issues with file validation or writing the parts
     * @see #split(File, int)
     * @see #splitBySize(File, long)
     * @see #splitBySize(File, long, File)
     */
    public static void split(final File file, final int countOfParts, final File destDir) throws IOException {
        final long sizeOfPart = (file.length() % countOfParts) == 0 ? (file.length() / countOfParts) : (file.length() / countOfParts) + 1;

        splitBySize(file, sizeOfPart, destDir);
    }

    /**
    * Splits a file into multiple parts based on the specified size per part.
    * <p>
    * This method divides the source file into sequential parts where each part (except possibly the last one)
    * has the specified size. The parts are saved as separate files in the same directory as the source file
    * with numbered suffixes. This is useful for creating file chunks for distribution, storage limitations,
    * or transfer purposes.
    * </p>
    *
    * <p>File naming convention:</p>
    * <ul>
    *   <li><strong>Pattern:</strong> {@code originalFileName_NNNN}</li>
    *   <li><strong>Numbering:</strong> Sequential 4-digit zero-padded numbers starting from 0001</li>
    *   <li><strong>Example:</strong> {@code document.pdf} becomes {@code document.pdf_0001}, {@code document.pdf_0002}, etc.</li>
    * </ul>
    *
    * <p>Size distribution:</p>
    * <ul>
    *   <li><strong>Regular parts:</strong> Each part (except the last) will be exactly {@code sizeOfPart} bytes</li>
    *   <li><strong>Last part:</strong> Contains the remaining bytes, which may be smaller than {@code sizeOfPart}</li>
    *   <li><strong>Empty files:</strong> if the source file is empty, one empty part file is created</li>
    * </ul>
    *
    * <p><b>Usage Examples:</b></p>
    * <pre>{@code
    * File sourceFile = new File("large_document.pdf");
    * 
    * // Split into 1MB parts
    * IOUtil.splitBySize(sourceFile, 1024 * 1024);
    * // Creates: large_document.pdf_0001, large_document.pdf_0002, etc.
    * 
    * // Split into 10KB parts
    * IOUtil.splitBySize(sourceFile, 10240);
    * }</pre>
    *
    * @param file the source file to split; must exist and be readable
    * @param sizeOfPart the maximum size in bytes for each part (except possibly the last part)
    * @throws IllegalArgumentException if {@code file} is {@code null}
    * @throws FileNotFoundException if the source file does not exist
    * @throws IOException if there are issues with file validation
    * @see #splitBySize(File, long, File)
    * @see #split(File, int, File)
    * @see #split(File, int)
    */
    public static void splitBySize(final File file, final long sizeOfPart) throws IOException {
        splitBySize(file, sizeOfPart, file.getParentFile());
    }

    /**
    * Splits a file into multiple parts based on the specified size per part and saves them to a destination directory.
    * <p>
    * This method divides the source file into sequential parts where each part (except possibly the last one)
    * has the specified size. The parts are saved as separate files in the destination directory with numbered
    * suffixes. This is useful for creating file chunks for distribution, storage limitations, or transfer purposes.
    * </p>
    *
    * <p>File naming convention:</p>
    * <ul>
    *   <li><strong>Pattern:</strong> {@code originalFileName_NNNN}</li>
    *   <li><strong>Numbering:</strong> Sequential 4-digit zero-padded numbers starting from 0001</li>
    *   <li><strong>Example:</strong> {@code document.pdf} becomes {@code document.pdf_0001}, {@code document.pdf_0002}, etc.</li>
    * </ul>
    *
    * <p>Size distribution:</p>
    * <ul>
    *   <li><strong>Regular parts:</strong> Each part (except the last) will be exactly {@code sizeOfPart} bytes</li>
    *   <li><strong>Last part:</strong> Contains the remaining bytes, which may be smaller than {@code sizeOfPart}</li>
    *   <li><strong>Empty files:</strong> if the source file is empty, one empty part file is created</li>
    * </ul>
    *
    * <p><b>Usage Examples:</b></p>
    * <pre>{@code
    * File sourceFile = new File("large_document.pdf");
    * File outputDir = new File("split_parts");
    * 
    * // Split into 1MB parts
    * IOUtil.splitBySize(sourceFile, 1024 * 1024, outputDir);
    * // Creates: large_document.pdf_0001, large_document.pdf_0002, etc.
    * 
    * // Split into 10KB parts
    * IOUtil.splitBySize(sourceFile, 10240, outputDir);
    * }</pre>
    *
    * @param file the source file to split; must exist and be readable
    * @param sizeOfPart the maximum size in bytes for each part (except possibly the last part)
    * @param destDir the destination directory where split parts will be saved; must exist and be writable
    * @throws IllegalArgumentException if {@code file} or {@code destDir} is {@code null}
    * @throws FileNotFoundException if the source file does not exist or the destination directory is invalid
    * @throws IOException if there are issues with file validation or directory access
    * @see #splitBySize(File, long)
    * @see #split(File, int, File)
    * @see #split(File, int)
    */
    public static void splitBySize(final File file, final long sizeOfPart, final File destDir) throws IOException {
        checkFileExists(file);
        checkDestDirectory(destDir);

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
     * @param file       the file to be split. This must be a valid file.
     * @param numOfParts the number of parts the file should be split into.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    static void splitByLine(final File file, final int numOfParts) throws UncheckedIOException {
        splitByLine(file, numOfParts, file.getParentFile());
    }

    /**
     * Splits the specified file into a number of parts by line.
     *
     * @param file       the file to be split. This must be a valid file.
     * @param numOfParts the number of parts the file should be split into.
     * @param destDir    the directory where the split parts will be stored.
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
                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
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
     * @param file the file whose line count is to be estimated, must not be {@code null}
     * @param byReadingLineNum the number of lines to read for estimating the total count.
     * @return the estimated total line count based on the sample size.
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
     * @param sourceFiles an array of files to be merged. These must be valid files.
     * @param destFile    the destination file where the merged content will be written. This must be a valid file.
     * @return the number of bytes written to the destination file.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    public static long merge(final File[] sourceFiles, final File destFile) throws UncheckedIOException {
        return merge(Array.asList(sourceFiles), destFile);
    }

    /**
     * Merges the given source files into the specified destination file.
     *
     * @param sourceFiles a collection of files to be merged. These must be valid files.
     * @param destFile    the destination file where the merged content will be written. This must be a valid file.
     * @return the number of bytes written to the destination file.
     * @throws UncheckedIOException if an I/O error occurs during the process.
     */
    public static long merge(final Collection<File> sourceFiles, final File destFile) throws UncheckedIOException {
        return merge(sourceFiles, N.EMPTY_BYTE_ARRAY, destFile);
    }

    /**
     * Merges the given source files into the specified destination file, separated by the provided delimiter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge split files back together
     * List<File> parts = Arrays.asList(
     *     new File("data.txt.1"),
     *     new File("data.txt.2"),
     *     new File("data.txt.3")
     * );
     * File merged = new File("data_merged.txt");
     * long bytesWritten = IOUtil.merge(parts, merged);
     *
     * // Merge with delimiter (e.g., newline between files)
     * IOUtil.merge(parts, "\n".getBytes(), merged);
     * }</pre>
     *
     * @param sourceFiles a collection of files to be merged. These must be valid files.
     * @param delimiter   a byte array that will be inserted between each file during the merge.
     * @param destFile    the destination file where the merged content will be written. This must be a valid file.
     * @return the number of bytes written to the destination file.
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
                    totalCount += delimiter.length;
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
     * Lists all files in the specified parent directory.
     *
     * @param parentPath the parent directory from which to list files.
     * @return a list of File objects representing all files in the parent directory.
     * @see Stream#listFiles(File)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static List<File> listFiles(final File parentPath) {
        return listFiles(parentPath, false, false);
    }

    /**
     * Lists all files in the specified parent directory.
     * if the recursive parameter is set to {@code true}, it will list files in all subdirectories as well.
     * if the excludeDirectory parameter is set to {@code true}, it will exclude directories from the list.
     *
     * @param parentPath       the parent directory from which to list files.
     * @param recursively      a boolean indicating whether to list files in all subdirectories.
     * @param excludeDirectory a boolean indicating whether to exclude directories from the list.
     * @return a list of File objects representing all files in the parent directory.
     * @see Stream#listFiles(File, boolean, boolean)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static List<File> listFiles(final File parentPath, final boolean recursively, final boolean excludeDirectory) {
        return listFiles(parentPath, recursively, excludeDirectory ? directories_excluded_filter : all_files_filter);
    }

    /**
     * Lists all files in the specified parent directory.
     * if the recursive parameter is set to {@code true}, it will list files in all subdirectories as well.
     *
     * @param <E>         the type of the exception that may be thrown by the filter.
     * @param parentPath  the parent directory where the listing will start. It must not be {@code null}.
     * @param recursively if {@code true}, files in all subdirectories of the parent directory will be listed.
     * @param filter      a BiPredicate that takes the parent directory and a file as arguments and returns a boolean. if the predicate returns {@code true}, the file is listed; if it returns {@code false}, the file is not listed.
     * @return a list of files in the specified directory and possibly its subdirectories.
     * @throws E if the filter throws an exception.
     * @see Stream#listFiles(File, boolean, boolean)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static <E extends Exception> List<File> listFiles(final File parentPath, final boolean recursively,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws E {
        N.checkArgNotNull(filter, cs.filter);

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
     * @param parentPath the parent directory from which to list directories.
     * @return a list of File objects representing all directories in the parent directory.
     * @see Stream#listFiles(File)
     * @see Fn#isDirectory()
     */
    public static List<File> listDirectories(final File parentPath) {
        return listDirectories(parentPath, false);
    }

    /**
     * Lists all directories in the specified parent directory.
     * if the recursive parameter is set to {@code true}, it will list directories in all subdirectories as well.
     *
     * @param parentPath  the parent directory from which to list directories.
     * @param recursively a boolean indicating whether to list directories in all subdirectories.
     * @return a list of File objects representing all directories in the parent directory and its subdirectories if recursively is {@code true}.
     * @see Stream#listFiles(File, boolean, boolean)
     * @see Fn#isDirectory()
     */
    public static List<File> listDirectories(final File parentPath, final boolean recursively) {
        return listFiles(parentPath, recursively, directories_only_filter);
    }

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
     * @param parentPath the parent directory from which to list files and directories.
     * @return a list of strings representing the names of all files and directories in the parent directory.
     * @see Stream#listFiles(File)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static Stream<File> walk(final File parentPath) {
        return walk(parentPath, false, false);
    }

    /**
     * Lists the names of all files and directories in the specified parent directory.
     * if the recursive parameter is set to {@code true}, it will list files in all subdirectories as well.
     * if the excludeDirectory parameter is set to {@code true}, it will exclude directories from the list.
     *
     * @param parentPath       the parent directory from which to list files and directories.
     * @param recursively      a boolean indicating whether to list files in all subdirectories.
     * @param excludeDirectory a boolean indicating whether to exclude directories from the list.
     * @return a list of strings representing the names of all files and directories in the parent directory.
     * @see Stream#listFiles(File, boolean, boolean)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static Stream<File> walk(final File parentPath, final boolean recursively, final boolean excludeDirectory) {
        return Stream.listFiles(parentPath, recursively, excludeDirectory);
    }

    //-----------------------------------------------------------------------

    /**
     * Convert from a {@code URL} to a {@code File}.
     * <p>
     * From version 1.1 this method will decode the URL.
     * Syntax such as {@code file:///my%20docs/file.txt} will be
     * correctly decoded to {@code /my docs/file.txt}. Starting with version
     * 1.5, this method uses UTF-8 to decode percent-encoded octets to characters.
     * Additionally, malformed percent-encoded octets are handled leniently by
     * passing them through literally.
     *
     * @param url the file URL to convert, {@code null} returns {@code null}
     * @return a File object corresponding to the input URL.
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
     * Decodes the specified URL as per RFC 3986, i.e., transforms
     * percent-encoded octets to characters by decoding with the UTF-8 character
     * set. This function is primarily intended for usage with
     * {@link java.net.URL} which unfortunately does not enforce proper URLs. As
     * such, this method will leniently accept invalid characters or malformed
     * percent-encoded octets and simply pass them literally through to the
     * result string. Except for rare edge cases, this will make unencoded URLs
     * pass through unaltered.
     *
     * @param url the URL to decode, may be {@code null}.
     * @return the decoded URL or {@code null} if the input was
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
     * Converts each {@code URL} in the specified array to a {@code File}.
     * <p>
     * Returns an array of the same size as the input.
     * if the input is {@code null}, an empty array is returned.
     * if the input contains {@code null}, the output array contains {@code null} at the same
     * index.
     * <p>
     * This method will decode the URL.
     * Syntax such as {@code file:///my%20docs/file.txt} will be
     * correctly decoded to {@code /my docs/file.txt}.
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
     * @param urls the collection of URLs to be converted.
     * @return a list of File objects corresponding to the input URLs.
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
     * @param file the File object to be converted.
     * @return a URL object corresponding to the input File object.
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
     * @param files the array of File objects to be converted.
     * @return an array of URL objects corresponding to the input File objects.
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
     * @param files the collection of File objects to be converted.
     * @return a list of URL objects corresponding to the input File objects.
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
     * Update the last modified time of the file to the system current time if the specified file exists.
     *
     * @param source the File to touch
     * @return {@code true} if the file exists and last modified time is updated successfully, {@code false} otherwise
     */
    public static boolean touch(final File source) {
        return source.exists() && source.setLastModified(System.currentTimeMillis());
    }

    /**
     * Tests whether the contents of two files are equal.
     * <p>
     * This method checks to see if the two files are different lengths or if they point to the same file, before
     * resorting to byte-by-byte comparison of the contents.
     * </p>
     *
     * @param file1 the first file
     * @param file2 the second file
     * @return {@code true} if the contents of the files are equal or they both don't exist, {@code false} otherwise
     * @throws IllegalArgumentException when an input is not a file.
     * @throws IOException if an I/O error occurs.
     */
    public static boolean contentEquals(final File file1, final File file2) throws IOException {
        if (file1 == file2) {
            return true;
        } else if (file1 == null || file2 == null) {
            return false;
        }

        final boolean file1Exists = file1.exists();

        if (file1Exists != file2.exists()) {
            return false;
        }

        if (!file1Exists) {
            // two not existing files are equal
            return true;
        }

        checkFileExists(file1);
        checkFileExists(file2);

        if (file1.length() != file2.length()) {
            // lengths differ, cannot be equal
            return false;
        }

        if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
            // same file
            return true;
        }

        try (final InputStream input1 = IOUtil.newFileInputStream(file1); //
                final InputStream input2 = IOUtil.newFileInputStream(file2)) {
            return contentEquals(input1, input2);
        }
    }

    /**
     * Compares the contents of two files to determine if they are equal or not.
     * <p>
     * This method checks to see if the two files point to the same file,
     * before resorting to line-by-line comparison of the contents.
     * </p>
     *
     * @param file1       the first file
     * @param file2       the second file
     * @param charsetName the name of the requested charset.
     *                    May be {@code null}, in which case the platform default is used
     * @return {@code true} if the content of the files are equal or neither exists,
     * {@code false} otherwise
     * @throws IllegalArgumentException when an input is not a file.
     * @throws IOException in case of an I/O error.
     * @throws UnsupportedCharsetException if the named charset is unavailable (unchecked exception).
     * @see IOUtil#contentEqualsIgnoreEOL(Reader, Reader)
     * @since 2.2
     */
    public static boolean contentEqualsIgnoreEOL(final File file1, final File file2, final String charsetName) throws IOException {
        if (file1 == file2) {
            return true;
        } else if (file1 == null || file2 == null) {
            return false;
        }

        final boolean file1Exists = file1.exists();

        if (file1Exists != file2.exists()) {
            return false;
        }

        if (!file1Exists) {
            // two not existing files are equal
            return true;
        }

        checkFileExists(file1);
        checkFileExists(file2);

        if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
            // same file
            return true;
        }

        final Charset charset = Strings.isEmpty(charsetName) ? DEFAULT_CHARSET : Charsets.get(charsetName);
        try (Reader input1 = new InputStreamReader(Files.newInputStream(file1.toPath()), charset);
                Reader input2 = new InputStreamReader(Files.newInputStream(file2.toPath()), charset)) {
            return contentEqualsIgnoreEOL(input1, input2);
        }
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
     * @return {@code true} if the content of the streams are equal, or they both don't
     * exist, {@code false} otherwise
     * @throws IOException if an I/O error occurs.
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
     * @return {@code true} if the content of the readers are equal, or they both don't exist, {@code false} otherwise
     * @throws IOException if an I/O error occurs.
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
     * Compares the contents of two Readers to determine if they are equal or not, ignoring EOL characters.
     * <p>
     * This method buffers the input internally using
     * {@link BufferedReader} if they are not already buffered.
     * </p>
     *
     * @param input1 the first reader
     * @param input2 the second reader
     * @return {@code true} if the content of the readers are equal (ignoring EOL differences),  {@code false} otherwise
     * @throws IOException if an I/O error occurs during the comparison
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
     * Parse the specified file/directory line by line.
     *
     * @param <E>        the type of exception that the lineAction can throw.
     * @param source     the source file to be parsed.
     * @param lineAction a Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if the lineAction throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final File source, final Throwables.Consumer<? super String, E> lineAction)
            throws UncheckedIOException, E {
        forLines(source, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param <E2> the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source the file or directory to process. If a directory, all files within it are processed recursively.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final File source, final Throwables.Consumer<? super String, E> lineAction,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        forLines(source, 0, Long.MAX_VALUE, lineAction, onComplete);
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param source the file or directory to process. If a directory, all files within it are processed recursively.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final File source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(source, lineOffset, count, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param <E2> the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source the file or directory to process. If a directory, all files within it are processed recursively.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final File source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        forLines(source, lineOffset, count, 0, 0, lineAction, onComplete);
    }

    /**
     * Parse the specified file/directory line by line with optional parallel processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple sequential processing
     * File file = new File("data.txt");
     * IOUtil.forLines(file, 0, Long.MAX_VALUE, 0, 0,
     *     line -> System.out.println(line));
     *
     * // Parallel processing with 4 worker threads
     * IOUtil.forLines(file, 0, Long.MAX_VALUE, 4, 1024,
     *     line -> processLine(line));
     *
     * // Skip first 100 lines, process next 1000 lines with 4 threads
     * IOUtil.forLines(file, 100, 1000, 4, 1024,
     *     line -> System.out.println("Processing: " + line));
     * }</pre>
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param source the file or directory to process. If a directory, all files within it are processed recursively.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count the maximum number of lines to process after the offset.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize the size of the buffer queue between reader and processor threads.
     * @param lineAction the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final File source, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(source, lineOffset, count, processThreadNum, queueSize, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>              the type of exception that the lineAction can throw.
     * @param <E2>             the type of exception that the onComplete can throw.
     * @param source           the source file/directory to be parsed.
     * @param lineOffset       the line number from where to start parsing.
     * @param count            the number of lines to be parsed.
     * @param processThreadNum the number of threads to be used for parsing.
     * @param queueSize        the size of the queue for holding lines to be parsed.
     * @param lineAction       a Consumer that takes a line of the file as a String and performs the desired operation.
     * @param onComplete       a Runnable that is executed after the parsing is complete.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if the lineAction throws an exception.
     * @throws E2                   if the onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final File source, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        forLines(source.isDirectory() ? listFiles(source, true, true) : Array.asList(source), lineOffset, count, processThreadNum, queueSize, lineAction,
                onComplete);
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>        the type of exception that the lineAction can throw.
     * @param files      the collection of files to be parsed.
     * @param lineAction a Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if the lineAction throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final Collection<File> files, final Throwables.Consumer<? super String, E> lineAction)
            throws UncheckedIOException, E {
        forLines(files, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param <E2> the type of exception that the onComplete callback may throw after all lines are processed.
     * @param files the collection of files to be parsed.
     * @param lineAction a Consumer that takes a line of the file as a String and performs the desired operation.
     * @param onComplete a Runnable that is executed after the parsing is complete.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final Collection<File> files,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        forLines(files, 0, Long.MAX_VALUE, lineAction, onComplete);
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param files      the collection of files/directories to process. Directories are processed recursively.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final Collection<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(files, lineOffset, count, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param files      the collection of files/directories to process. Directories are processed recursively.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final Collection<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        forLines(files, lineOffset, count, 0, 0, lineAction, onComplete);
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param files            the collection of files/directories to process. Directories are processed recursively.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final Collection<File> files, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(files, lineOffset, count, processThreadNum, queueSize, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>        the type of exception that the lineAction can throw.
     * @param <E2>       the type of exception that the onComplete can throw.
     * @param files      the collection of files to be parsed.
     * @param lineOffset the line number from where to start parsing.
     * @param count      the number of lines to be parsed.
     * @param processThreadNum the number of threads to use for parallel processing
     * @param queueSize  the size of the queue for buffering lines during parallel processing
     * @param lineAction a Consumer that takes a line of the file as a String and performs the desired operation.
     * @param onComplete a Runnable that is executed after the parsing is complete.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if the lineAction throws an exception.
     * @throws E2                   if the onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final Collection<File> files, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
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

            Iterators.forEach(iterators, lineOffset, count, 0, processThreadNum, queueSize, lineAction, onComplete);
        } finally {
            for (final Reader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param source           the file or directory to process. Directories are processed recursively.
     * @param readThreadNum    the number of threads for reading files.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final File source, final int readThreadNum, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(source, readThreadNum, processThreadNum, queueSize, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param <E2>             the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source           the file or directory to process. Directories are processed recursively.
     * @param readThreadNum    the number of threads for reading files.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @param onComplete       the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final File source, final int readThreadNum, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        forLines(source, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, lineAction, onComplete);
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param source           the file or directory to process. Directories are processed recursively.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param readThreadNum    the number of threads for reading files.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final File source, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(source, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified file/directory line by line.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param <E2>             the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source           the file or directory to process. All subfiles are processed recursively if this is a directory.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param readThreadNum    the number of threads for reading files.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @param onComplete       the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final File source, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        forLines(source.isDirectory() ? listFiles(source, true, true) : Array.asList(source), lineOffset, count, readThreadNum, processThreadNum, queueSize,
                lineAction, onComplete);
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param files            the collection of files/directories to process. Directories are processed recursively.
     * @param readThreadNum    the number of threads for reading files.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final Collection<File> files, final int readThreadNum, final int processThreadNum, final int queueSize,
            final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(files, readThreadNum, processThreadNum, queueSize, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param <E2>             the type of exception that the onComplete callback may throw after all lines are processed.
     * @param files            the collection of files/directories to process. Directories are processed recursively.
     * @param readThreadNum    the number of threads for reading files.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @param onComplete       the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final Collection<File> files, final int readThreadNum, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        forLines(files, 0, Long.MAX_VALUE, readThreadNum, processThreadNum, queueSize, lineAction, onComplete);
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param files            the collection of files/directories to process. Directories are processed recursively.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param readThreadNum    the number of threads for reading files.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final Collection<File> files, final long lineOffset, final long count, final int readThreadNum,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(files, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the given collection of file line by line using the provided lineAction.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param <E2>             the type of exception that the onComplete callback may throw after all lines are processed.
     * @param files            the collection of files/directories to process. All subfiles are processed recursively if an element is a directory.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param readThreadNum    the number of threads for reading files.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @param onComplete       the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final Collection<File> files, final long lineOffset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction,
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

            Iterators.forEach(iterators, lineOffset, count, readThreadNum, processThreadNum, queueSize, lineAction, onComplete);
        } finally {
            for (final Reader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the InputStream to read lines from.
     * @param lineAction the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final InputStream source, final Throwables.Consumer<? super String, E> lineAction)
            throws UncheckedIOException, E {
        forLines(source, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the InputStream to read lines from.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final InputStream source, final Throwables.Consumer<? super String, E> lineAction,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        forLines(source, 0, Long.MAX_VALUE, lineAction, onComplete);
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the InputStream to read lines from.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final InputStream source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(source, lineOffset, count, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the InputStream to read lines from.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final InputStream source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        forLines(source, lineOffset, count, 0, 0, lineAction, onComplete);
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param source           the InputStream to read lines from.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final InputStream source, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(source, lineOffset, count, processThreadNum, queueSize, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified InputStream line by line.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param <E2>             the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source           the InputStream to read lines from.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @param onComplete       the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final InputStream source, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        final BufferedReader br = Objectory.createBufferedReader(source);

        try {
            forLines(br, lineOffset, count, processThreadNum, queueSize, lineAction, onComplete);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the Reader to read lines from.
     * @param lineAction the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final Reader source, final Throwables.Consumer<? super String, E> lineAction)
            throws UncheckedIOException, E {
        forLines(source, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the Reader to read lines from.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final Reader source, final Throwables.Consumer<? super String, E> lineAction,
            final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        forLines(source, 0, Long.MAX_VALUE, lineAction, onComplete);
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the Reader to read lines from.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final Reader source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(source, lineOffset, count, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the Reader to read lines from.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final Reader source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete) throws UncheckedIOException, E, E2 {
        forLines(source, lineOffset, count, 0, 0, lineAction, onComplete);
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param source           the Reader to read lines from.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forLines(Reader, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forLines(final Reader source, final long lineOffset, final long count, final int processThreadNum,
            final int queueSize, final Throwables.Consumer<? super String, E> lineAction) throws UncheckedIOException, E {
        forLines(source, lineOffset, count, processThreadNum, queueSize, lineAction, Fn.emptyAction());
    }

    /**
     * Parse the specified Reader line by line.
     *
     * @param <E>              the type of exception that the lineAction may throw during line processing.
     * @param <E2>             the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source           the Reader to read lines from.
     * @param lineOffset       the number of lines to skip from the beginning.
     * @param count            the maximum number of lines to process after the offset.
     * @param processThreadNum the number of worker threads for parallel processing. 0 or negative for sequential processing.
     * @param queueSize        the size of the buffer queue between reader and processor threads.
     * @param lineAction       the action to perform on each line.
     * @param onComplete       the action to perform after all lines have been processed successfully.
     * @throws UncheckedIOException if an I/O error occurs.
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see Iterators#forEach(Iterator, long, long, int, int, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forLines(final Reader source, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws UncheckedIOException, E, E2 {
        Iterators.forEach(new LineIterator(source), lineOffset, count, processThreadNum, queueSize, lineAction, onComplete);
    }

    /**
     * Opens a file and returns an InputStream. Handles .gz and .zip files automatically.
     *
     * @param source the file to open, must not be {@code null}
     * @param outputZipFile a holder for the ZipFile if the source is a .zip file.
     * @return an InputStream for reading the file.
     * @throws IOException if an I/O error occurs.
     */
    private static InputStream openFile(final File source, final Holder<ZipFile> outputZipFile) throws IOException {
        InputStream is = null;

        if (source.getName().endsWith(GZ)) {
            final FileInputStream fis = IOUtil.newFileInputStream(source);
            try {
                is = new GZIPInputStream(fis);
            } catch (IOException e) {
                close(fis);
                throw e;
            }
        } else if (source.getName().endsWith(ZIP)) {
            final ZipFile zf = new ZipFile(source);
            try {
                final ZipEntry ze = zf.entries().nextElement();
                is = zf.getInputStream(ze);
                outputZipFile.setValue(zf);
            } catch (IOException e) {
                close(zf);
                throw e;
            }
        } else {
            is = IOUtil.newFileInputStream(source);
        }

        return is;
    }

    /**
     * Converts a CharSequence to a byte array using the specified charset.
     *
     * @param cs      the CharSequence to convert.
     * @param charset the charset to use for encoding.
     * @return the byte array representation of the CharSequence.
     */
    private static byte[] toByteArray(final CharSequence cs, final Charset charset) {
        return String.valueOf(cs).getBytes(charset);
    }
}
