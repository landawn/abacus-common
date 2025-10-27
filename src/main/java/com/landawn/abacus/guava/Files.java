/*
 * Copyright (C) 2007 The Guava Authors
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
package com.landawn.abacus.guava;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.graph.Traverser;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.InsecureRecursiveDeleteException;
import com.google.common.io.RecursiveDeleteOption;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableList;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 * 
 * A utility class that provides unified file operations by wrapping Google Guava's file utilities.
 * This class serves as a facade to provide consistent APIs for file manipulation, combining
 * functionality from both {@code com.google.common.io.Files} and {@code com.google.common.io.MoreFiles}.
 * 
 * <p>This class offers various file operations including:
 * <ul>
 *   <li>Reading and writing files with different character sets</li>
 *   <li>File comparison and manipulation</li>
 *   <li>Directory traversal and management</li>
 *   <li>Memory-mapped file operations</li>
 *   <li>Path simplification and file extension handling</li>
 * </ul>
 * 
 * <p>All methods in this class are static. The class cannot be instantiated.
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Read all lines from a file
 * List<String> lines = Files.readAllLines(new File("data.txt"), StandardCharsets.UTF_8);
 * 
 * // Copy a file
 * Files.copy(new File("source.txt"), new File("dest.txt"));
 * }</pre>
 *
 * @see java.nio.file.Files
 * @see com.google.common.io.Files
 * @see com.google.common.io.MoreFiles
 * @see com.landawn.abacus.util.IOUtil
 * @see com.landawn.abacus.util.Strings
 */
public abstract class Files { //NOSONAR

    private Files() {
        // singleton
    }

    /**
     * Returns a buffered reader that reads from a file using the given character set.
     * This method is a convenience wrapper around {@link com.google.common.io.Files#newReader(File, Charset)}.
     *
     * <p>The returned reader will use a default buffer size. The reader should be closed
     * after use to release system resources.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#newBufferedReader(java.nio.file.Path, Charset)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (BufferedReader reader = Files.newReader(new File("data.txt"), StandardCharsets.UTF_8)) {
     *     String line;
     *     while ((line = reader.readLine()) != null) {
     *         System.out.println(line);
     *     }
     * }
     * }</pre>
     *
     * @param file the file to read from
     * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return the buffered reader
     * @throws FileNotFoundException if the file does not exist, is a directory rather than a regular file,
     *     or for some other reason cannot be opened for reading
     */
    public static BufferedReader newReader(final File file, final Charset charset) throws FileNotFoundException {
        return com.google.common.io.Files.newReader(file, charset);
    }

    /**
     * Returns a buffered writer that writes to a file using the given character set.
     * If the file already exists, it will be overwritten. If the file does not exist, it will be created.
     * This method is a convenience wrapper around {@link com.google.common.io.Files#newWriter(File, Charset)}.
     *
     * <p>The returned writer will use a default buffer size. The writer should be closed
     * after use to ensure all data is written and to release system resources.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#newBufferedWriter(java.nio.file.Path, Charset,
     * java.nio.file.OpenOption...)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (BufferedWriter writer = Files.newWriter(new File("output.txt"), StandardCharsets.UTF_8)) {
     *     writer.write("Hello, World!");
     *     writer.newLine();
     * }
     * }</pre>
     *
     * @param file the file to write to
     * @param charset the charset used to encode the output stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return the buffered writer
     * @throws FileNotFoundException if the file exists but is a directory rather than a regular file,
     *     does not exist but cannot be created, or cannot be opened for any other reason
     */
    public static BufferedWriter newWriter(final File file, final Charset charset) throws FileNotFoundException {
        return com.google.common.io.Files.newWriter(file, charset);
    }

    /**
     * Returns a new {@link ByteSource} for reading bytes from the given file.
     * The returned ByteSource is a view of the file that allows for efficient byte-level operations.
     *
     * <p>This method does not open the file immediately. The file is opened when methods
     * on the returned ByteSource are called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteSource source = Files.asByteSource(new File("data.bin"));
     * byte[] bytes = source.read();
     * }</pre>
     *
     * @param file the file to create a ByteSource for
     * @return a ByteSource that reads from the given file
     */
    public static ByteSource asByteSource(final File file) {
        return com.google.common.io.Files.asByteSource(file);
    }

    /**
     * Returns a view of the given {@code path} as a {@link ByteSource}.
     * This method provides more control over how the file is accessed through open options.
     *
     * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
     * and may affect the behavior of the returned source and the streams it provides. See {@link
     * StandardOpenOption} for the standard options that may be provided. Providing no options is
     * equivalent to providing the {@link StandardOpenOption#READ READ} option.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("data.bin");
     * ByteSource source = Files.asByteSource(path, StandardOpenOption.READ);
     * long size = source.size();
     * }</pre>
     *
     * @param path the path to create a ByteSource for
     * @param options zero or more open options that control how the file is opened
     * @return a ByteSource that reads from the given path
     */
    public static ByteSource asByteSource(final Path path, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asByteSource(path, options);
    }

    /**
     * Returns a new {@link ByteSink} for writing bytes to the given file. The given {@code modes}
     * control how the file is opened for writing. When no mode is provided, the file will be
     * truncated before writing. When the {@link FileWriteMode#APPEND APPEND} mode is provided, writes
     * will append to the end of the file without truncating it.
     *
     * <p>This method does not open the file immediately. The file is opened when methods
     * on the returned ByteSink are called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Overwrite file
     * ByteSink sink = Files.asByteSink(new File("output.bin"));
     * sink.write(byteArray);
     * 
     * // Append to file
     * ByteSink appendSink = Files.asByteSink(new File("log.bin"), FileWriteMode.APPEND);
     * appendSink.write(moreBytes);
     * }</pre>
     *
     * @param file the file to create a ByteSink for
     * @param modes optional file write modes; if empty, the file will be truncated
     * @return a ByteSink that writes to the given file
     */
    public static ByteSink asByteSink(final File file, final FileWriteMode... modes) {
        return com.google.common.io.Files.asByteSink(file, modes);
    }

    /**
     * Returns a view of the given {@code path} as a {@link ByteSink}.
     * This method provides fine-grained control over how the file is opened for writing.
     *
     * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
     * and may affect the behavior of the returned sink and the streams it provides. See {@link
     * StandardOpenOption} for the standard options that may be provided. Providing no options is
     * equivalent to providing the {@link StandardOpenOption#CREATE CREATE}, {@link
     * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} and {@link StandardOpenOption#WRITE
     * WRITE} options.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("output.bin");
     * ByteSink sink = Files.asByteSink(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
     * sink.write(byteArray);
     * }</pre>
     *
     * @param path the path to create a ByteSink for
     * @param options zero or more open options that control how the file is opened
     * @return a ByteSink that writes to the given path
     */
    public static ByteSink asByteSink(final Path path, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asByteSink(path, options);
    }

    /**
     * Returns a new {@link CharSource} for reading character data from the given file using the given
     * character set. The returned CharSource is a view of the file that allows for efficient
     * character-level operations.
     *
     * <p>This method does not open the file immediately. The file is opened when methods
     * on the returned CharSource are called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSource source = Files.asCharSource(new File("text.txt"), StandardCharsets.UTF_8);
     * String content = source.read();
     * List<String> lines = source.readLines();
     * }</pre>
     *
     * @param file the file to create a CharSource for
     * @param charset the character set to use when reading the file
     * @return a CharSource that reads from the given file using the specified charset
     */
    public static CharSource asCharSource(final File file, final Charset charset) {
        return com.google.common.io.Files.asCharSource(file, charset);
    }

    /**
     * Returns a view of the given {@code path} as a {@link CharSource} using the given {@code
     * charset}. This method provides more control over how the file is accessed through open options.
     *
     * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
     * and may affect the behavior of the returned source and the streams it provides. See {@link
     * StandardOpenOption} for the standard options that may be provided. Providing no options is
     * equivalent to providing the {@link StandardOpenOption#READ READ} option.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("text.txt");
     * CharSource source = Files.asCharSource(path, StandardCharsets.UTF_8, StandardOpenOption.READ);
     * try (BufferedReader reader = source.openBufferedStream()) {
     *     // Process lines
     * }
     * }</pre>
     *
     * @param path the path to create a CharSource for
     * @param charset the character set to use when reading the file
     * @param options zero or more open options that control how the file is opened
     * @return a CharSource that reads from the given path using the specified charset
     */
    public static CharSource asCharSource(final Path path, final Charset charset, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asCharSource(path, charset, options);
    }

    /**
     * Returns a new {@link CharSink} for writing character data to the given file using the given
     * character set. The given {@code modes} control how the file is opened for writing. When no mode
     * is provided, the file will be truncated before writing. When the {@link FileWriteMode#APPEND
     * APPEND} mode is provided, writes will append to the end of the file without truncating it.
     *
     * <p>This method does not open the file immediately. The file is opened when methods
     * on the returned CharSink are called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Overwrite file
     * CharSink sink = Files.asCharSink(new File("output.txt"), StandardCharsets.UTF_8);
     * sink.write("Hello, World!");
     * 
     * // Append to file
     * CharSink appendSink = Files.asCharSink(new File("log.txt"), StandardCharsets.UTF_8, FileWriteMode.APPEND);
     * appendSink.writeLines(Arrays.asList("Line 1", "Line 2"));
     * }</pre>
     *
     * @param file the file to create a CharSink for
     * @param charset the character set to use when writing to the file
     * @param modes optional file write modes; if empty, the file will be truncated
     * @return a CharSink that writes to the given file using the specified charset
     */
    public static CharSink asCharSink(final File file, final Charset charset, final FileWriteMode... modes) {
        return com.google.common.io.Files.asCharSink(file, charset, modes);
    }

    /**
     * Returns a view of the given {@code path} as a {@link CharSink} using the given {@code charset}.
     * This method provides fine-grained control over how the file is opened for writing.
     *
     * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
     * and may affect the behavior of the returned sink and the streams it provides. See {@link
     * StandardOpenOption} for the standard options that may be provided. Providing no options is
     * equivalent to providing the {@link StandardOpenOption#CREATE CREATE}, {@link
     * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} and {@link StandardOpenOption#WRITE
     * WRITE} options.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("output.txt");
     * CharSink sink = Files.asCharSink(path, StandardCharsets.UTF_8, 
     *     StandardOpenOption.CREATE, StandardOpenOption.APPEND);
     * sink.write("Appended text");
     * }</pre>
     *
     * @param path the path to create a CharSink for
     * @param charset the character set to use when writing to the file
     * @param options zero or more open options that control how the file is opened
     * @return a CharSink that writes to the given path using the specified charset
     */
    public static CharSink asCharSink(final Path path, final Charset charset, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asCharSink(path, charset, options);
    }

    /**
     * Reads all bytes from a file into a byte array.
     * This method ensures that the file is closed when all bytes have been read
     * or an I/O error, or other runtime exception, is thrown.
     *
     * <p>This method is intended for simple cases where it is convenient to read all bytes
     * into a byte array. It is not intended for reading large files.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] fileData = Files.toByteArray(new File("image.png"));
     * }</pre>
     *
     * @param file the file to read
     * @return a byte array containing all bytes from the file
     * @throws IOException if an I/O error occurs while reading the file
     * @throws OutOfMemoryError if the file is larger than the available heap space
     */
    public static byte[] toByteArray(final File file) throws IOException {
        return com.google.common.io.Files.toByteArray(file);
    }

    /**
     * Overwrites a file with the contents of a byte array.
     * If the file already exists, it will be overwritten. If the file does not exist,
     * it will be created along with any necessary parent directories.
     *
     * <p>This method is equivalent to {@code asByteSink(to).write(from)}.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#write(java.nio.file.Path, byte[], java.nio.file.OpenOption...)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
     * Files.write(data, new File("output.txt"));
     * }</pre>
     *
     * @param from the bytes to write
     * @param to the destination file
     * @throws FileNotFoundException if the parent directory of {@code to} doesn't exist
     * @throws IOException if an I/O error occurs while writing to the file
     */
    public static void write(final byte[] from, final File to) throws IOException {
        com.google.common.io.Files.write(from, to);
    }

    /**
     * Returns {@code true} if the given files exist, are not directories, and contain the same bytes.
     * This method performs a byte-by-byte comparison of the file contents.
     *
     * <p>This method may be faster than reading both files into memory for comparison,
     * as it can stop reading as soon as a difference is found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean identical = Files.equal(new File("file1.txt"), new File("file2.txt"));
     * if (identical) {
     *     System.out.println("Files are identical");
     * }
     * }</pre>
     *
     * @param file1 the first file to compare
     * @param file2 the second file to compare
     * @return {@code true} if the files contain the same bytes, {@code false} otherwise
     * @throws IOException if an I/O error occurs while reading either file
     */
    public static boolean equal(final File file1, final File file2) throws IOException { //NOSONAR
        return com.google.common.io.Files.equal(file1, file2);
    }

    /**
     * Returns {@code true} if the files located by the given paths exist, are not directories, and contain
     * the same bytes. This method performs a byte-by-byte comparison of the file contents.
     *
     * <p>This method may be faster than reading both files into memory for comparison,
     * as it can stop reading as soon as a difference is found.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path1 = Paths.get("file1.txt");
     * Path path2 = Paths.get("file2.txt");
     * boolean identical = Files.equal(path1, path2);
     * }</pre>
     *
     * @param path1 the first path to compare
     * @param path2 the second path to compare
     * @return {@code true} if the files contain the same bytes, {@code false} otherwise
     * @throws IOException if an I/O error occurs while reading either file
     */
    public static boolean equal(final Path path1, final Path path2) throws IOException { //NOSONAR
        return com.google.common.io.MoreFiles.equal(path1, path2);
    }

    /**
     * Creates an empty file or updates the last updated timestamp on the same as the unix command of
     * the same name. If the file already exists, its last modified time will be updated to the
     * current time. If the file does not exist, an empty file will be created.
     *
     * <p>This method creates parent directories if necessary.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("timestamp.txt");
     * Files.touch(file);
     * // File now exists (if it didn't) and has current timestamp
     * }</pre>
     *
     * @param file the file to create or update
     * @throws IOException if an I/O error occurs while creating or updating the file
     */
    public static void touch(final File file) throws IOException {
        com.google.common.io.Files.touch(file);
    }

    /**
     * Like the unix command of the same name, creates an empty file or updates the last modified
     * timestamp of the existing file at the given path to the current system time.
     * If the file already exists, its last modified time will be updated. If the file does not
     * exist, an empty file will be created.
     *
     * <p>This method creates parent directories if necessary.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("timestamp.txt");
     * Files.touch(path);
     * // File now exists (if it didn't) and has current timestamp
     * }</pre>
     *
     * @param path the path of the file to create or update
     * @throws IOException if an I/O error occurs while creating or updating the file
     */
    public static void touch(final Path path) throws IOException {
        com.google.common.io.MoreFiles.touch(path);
    }

    /**
     * Atomically creates a new directory somewhere beneath the system's temporary directory (as
     * defined by the {@code java.io.tmpdir} system property), and returns its name.
     *
     * <p>The temporary directory is created with permissions restricted to the current user or, in
     * the case of Android, the current app. If that is not possible (as is the case under the very
     * old Android Ice Cream Sandwich release), then this method throws an exception instead of
     * creating a directory that would be more accessible. (This behavior is new in Guava 32.0.0.
     * Previous versions would create a directory that is more accessible, as discussed in
     * <a href="https://github.com/google/guava/issues/4011">CVE-2020-8908</a>.)
     *
     * <p>Use this method instead of {@link File#createTempFile(String, String)} when you wish to
     * create a directory, not a regular file. A common pitfall is to call {@code createTempFile},
     * delete the file and create a directory in its place, but this leads a race condition which can
     * be exploited to create security vulnerabilities, especially when executable files are to be
     * written into the directory.
     *
     * <p>This method assumes that the temporary volume is writable, has free inodes and free blocks,
     * and that it will not be called thousands of times per second.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#createTempDirectory}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File tempDir = Files.createTempDir();
     * // Use tempDir for temporary operations
     * // Remember to delete when done
     * }</pre>
     *
     * @return the newly created directory
     * @throws IllegalStateException if the directory could not be created, such as if the system does
     *     not support creating temporary directories securely
     * @deprecated For Android users, see the
     *     <a href="https://developer.android.com/training/data-storage" target="_blank">Data and File
     *     Storage overview</a> to select an appropriate temporary directory (perhaps {@code
     *     context.getCacheDir()}), and create your own directory under that. (For example, you might
     *     use {@code new File(context.getCacheDir(), "directoryName").mkdir()}, or, if you need an
     *     arbitrary number of temporary directories, you might have to generate multiple directory
     *     names in a loop until {@code mkdir()} returns {@code true}.) For JRE users, prefer {@link
     *     java.nio.file.Files#createTempDirectory}, transforming it to a {@link File} using {@link
     *     java.nio.file.Path#toFile() toFile()} if needed. To restrict permissions as this method
     *     does, pass {@code
     *     PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"))} to your
     *     call to {@code createTempDirectory}.
     */
    @Deprecated
    public static File createTempDir() {
        return com.google.common.io.Files.createTempDir(); //NOSONAR
    }

    /**
     * Creates any necessary but nonexistent parent directories of the specified file. Note that if
     * this operation fails, it may have succeeded in creating some (but not all) of the necessary
     * parent directories.
     *
     * <p>This method does nothing if the parent directory already exists. Unlike the similar
     * {@link File#mkdirs()}, this method throws an exception if the parent directory cannot
     * be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("/path/to/deep/directory/file.txt");
     * Files.createParentDirs(file);
     * // Now /path/to/deep/directory/ exists
     * }</pre>
     *
     * @param file the file whose parent directories should be created
     * @throws IOException if an I/O error occurs, or if any necessary but nonexistent parent
     *     directories of the specified file could not be created
     */
    public static void createParentDirs(final File file) throws IOException {
        com.google.common.io.Files.createParentDirs(file);
    }

    /**
     * Creates any necessary but nonexistent parent directories of the specified path. Note that if
     * this operation fails, it may have succeeded in creating some (but not all) of the necessary
     * parent directories. The parent directory is created with the given {@code attrs}.
     *
     * <p>This method does nothing if the parent directory already exists. File attributes
     * can be used to set permissions or other properties on the created directories.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("/path/to/deep/directory/file.txt");
     * Files.createParentDirectories(path);
     * // Now /path/to/deep/directory/ exists
     * }</pre>
     *
     * @param path the path whose parent directories should be created
     * @param attrs an optional list of file attributes to set atomically when creating the directories
     * @throws IOException if an I/O error occurs, or if any necessary but nonexistent parent
     *     directories of the specified file could not be created
     */
    public static void createParentDirectories(final Path path, final FileAttribute<?>... attrs) throws IOException {
        com.google.common.io.MoreFiles.createParentDirectories(path, attrs);
    }

    /**
     * Copies all bytes from a file to an output stream.
     * This method opens the file, copies all bytes to the output stream, and then closes
     * the file (but not the output stream).
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#copy(java.nio.file.Path, OutputStream)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileOutputStream out = new FileOutputStream("backup.bin")) {
     *     Files.copy(new File("original.bin"), out);
     * }
     * }</pre>
     *
     * @param from the source file
     * @param to the output stream to write to
     * @throws IOException if an I/O error occurs while reading from the file or writing to the stream
     */
    public static void copy(final File from, final OutputStream to) throws IOException {
        com.google.common.io.Files.copy(from, to);
    }

    /**
     * Copies all the bytes from one file to another.
     * This method will overwrite an existing destination file.
     *
     * <p>Copying is not an atomic operation - in the case of an I/O error, power loss, process
     * termination, or other problems, {@code to} may not be a complete copy of {@code from}. If you
     * need to guard against those conditions, you should employ other file-level synchronization.
     *
     * <p><b>Warning:</b> If {@code to} represents an existing file, that file will be overwritten
     * with the contents of {@code from}. If {@code to} and {@code from} refer to the <i>same</i>
     * file, the contents of that file will be deleted.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption...)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("document.pdf");
     * File backup = new File("document_backup.pdf");
     * Files.copy(source, backup);
     * }</pre>
     *
     * @param from the source file
     * @param to the destination file
     * @throws IOException if an I/O error occurs during the copy operation
     * @throws IllegalArgumentException if {@code from.equals(to)}
     */
    public static void copy(final File from, final File to) throws IOException {
        com.google.common.io.Files.copy(from, to);
    }

    /**
     * Moves a file from one path to another. This method can rename a file and/or move it to a
     * different directory. In either case {@code to} must be the target path for the file itself; not
     * just the new name for the file or the path to the new parent directory.
     *
     * <p>This method attempts to move the file atomically, but falls back to a copy-and-delete
     * operation if an atomic move is not supported by the file system.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link java.nio.file.Files#move}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Rename a file
     * Files.move(new File("old_name.txt"), new File("new_name.txt"));
     * 
     * // Move to different directory
     * Files.move(new File("file.txt"), new File("archive/file.txt"));
     * }</pre>
     *
     * @param from the source file
     * @param to the destination file (must be the complete target path, not just a directory)
     * @throws IOException if an I/O error occurs during the move operation
     * @throws IllegalArgumentException if {@code from.equals(to)}
     */
    public static void move(final File from, final File to) throws IOException {
        com.google.common.io.Files.move(from, to);
    }

    /**
     * Reads all the lines from a file. The lines do not include line-termination characters, but
     * do include other leading and trailing whitespace.
     *
     * <p>This method returns a mutable {@code List}. For an {@code ImmutableList}, use {@code
     * Files.asCharSource(file, charset).readLines()}.
     *
     * <p>This method is intended for simple cases where it is convenient to read all lines
     * in a single operation. It is not intended for reading large files.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#readAllLines(java.nio.file.Path, Charset)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> lines = Files.readLines(new File("data.txt"), StandardCharsets.UTF_8);
     * for (String line : lines) {
     *     System.out.println(line);
     * }
     * }</pre>
     *
     * @param file the file to read from
     * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return a mutable {@link List} containing all the lines
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static List<String> readLines(final File file, final Charset charset) throws IOException {
        return com.google.common.io.Files.readLines(file, charset);
    }

    /**
     * Fully maps a file read-only in to memory as per {@link
     * FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}.
     * This creates a memory-mapped byte buffer that reflects the contents of the file.
     *
     * <p>Files are mapped from offset 0 to its length.
     *
     * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
     *
     * <p>Memory-mapped files can offer significant performance benefits for certain operations,
     * but they also consume virtual memory space and may not be suitable for all use cases.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MappedByteBuffer buffer = Files.map(new File("data.bin"));
     * int firstInt = buffer.getInt();
     * }</pre>
     *
     * @param file the file to map
     * @return a read-only buffer reflecting {@code file}
     * @throws FileNotFoundException if the {@code file} does not exist
     * @throws IOException if an I/O error occurs
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file) throws IOException {
        return com.google.common.io.Files.map(file);
    }

    /**
     * Fully maps a file in to memory as per {@link
     * FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)} using the requested {@link
     * MapMode}. This allows for read-only, read-write, or private (copy-on-write) mappings.
     *
     * <p>Files are mapped from offset 0 to its length.
     *
     * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a read-write mapping
     * MappedByteBuffer buffer = Files.map(new File("data.bin"), MapMode.READ_WRITE);
     * buffer.putInt(0, 42); // Write to the beginning of the file
     * }</pre>
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file} (READ_ONLY, READ_WRITE, or PRIVATE)
     * @return a buffer reflecting {@code file}
     * @throws FileNotFoundException if the {@code file} does not exist
     * @throws IOException if an I/O error occurs
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file, final MapMode mode) throws IOException {
        return com.google.common.io.Files.map(file, mode);
    }

    /**
     * Maps a file in to memory as per {@link FileChannel#map(java.nio.channels.FileChannel.MapMode,
     * long, long)} using the requested {@link MapMode}. This method allows you to specify the
     * size of the mapping, which can be different from the file's current size.
     *
     * <p>Files are mapped from offset 0 to {@code size}.
     *
     * <p>If the mode is {@link MapMode#READ_WRITE} and the file does not exist, it will be created
     * with the requested {@code size}. Thus, this method is useful for creating memory mapped files
     * which do not yet exist.
     *
     * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a new 1MB memory-mapped file
     * MappedByteBuffer buffer = Files.map(new File("new.bin"), MapMode.READ_WRITE, 1024 * 1024);
     * }</pre>
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file}
     * @param size the number of bytes to map starting from offset 0
     * @return a buffer reflecting {@code file}
     * @throws IOException if an I/O error occurs
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file, final MapMode mode, final long size) throws IOException {
        return com.google.common.io.Files.map(file, mode, size);
    }

    /**
     * Returns the lexically cleaned form of the path name, <i>usually</i> (but not always) equivalent
     * to the original. The following heuristics are used:
     *
     * <ul>
     *   <li>empty string becomes .
     *   <li>. stays as .
     *   <li>fold out ./
     *   <li>fold out ../ when possible
     *   <li>collapse multiple slashes
     *   <li>delete trailing slashes (unless the path is just "/")
     * </ul>
     *
     * <p>These heuristics do not always match the behavior of the filesystem. In particular, consider
     * the path {@code a/../b}, which {@code simplifyPath} will change to {@code b}. If {@code a} is a
     * symlink to {@code x}, {@code a/../b} may refer to a sibling of {@code x}, rather than the
     * sibling of {@code a} referred to by {@code b}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String cleaned = Files.simplifyPath("/foo//bar/../baz/./");
     * // Returns: "/foo/baz"
     * }</pre>
     *
     * @param pathname the path to simplify
     * @return the simplified path
     */
    public static String simplifyPath(final String pathname) {
        return com.google.common.io.Files.simplifyPath(pathname);
    }

    /**
     * Returns the file name without its
     * <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
     * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String name = Files.getNameWithoutExtension("/home/user/document.pdf");
     * // Returns: "document"
     * 
     * String name2 = Files.getNameWithoutExtension("archive.tar.gz");
     * // Returns: "archive.tar"
     * }</pre>
     *
     * @param file The name of the file to trim the extension from. This can be either a fully
     *     qualified file name (including a path) or just a file name.
     * @return The file name without its path or extension.
     */
    public static String getNameWithoutExtension(final String file) {
        return com.google.common.io.Files.getNameWithoutExtension(file);
    }

    /**
     * Returns the file name without its
     * <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
     * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("/home/user/document.pdf");
     * String name = Files.getNameWithoutExtension(path);
     * // Returns: "document"
     * }</pre>
     *
     * @param path the path whose file name should be extracted without extension
     * @return the file name without its extension
     */
    public static String getNameWithoutExtension(final Path path) {
        return com.google.common.io.MoreFiles.getNameWithoutExtension(path);
    }

    /**
     * Returns the <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> for
     * the given file name, or the empty string if the file has no extension. The result does not
     * include the '{@code .}'.
     *
     * <p><b>Note:</b> This method simply returns everything after the last '{@code .}' in the file's
     * name as determined by {@link File#getName}. It does not account for any filesystem-specific
     * behavior that the {@link File} API does not already account for. For example, on NTFS it will
     * report {@code "txt"} as the extension for the filename {@code "foo.exe:.txt"} even though NTFS
     * will drop the {@code ":.txt"} part of the name when the file is actually created on the
     * filesystem due to NTFS's
     * <a href="https://learn.microsoft.com/en-us/archive/blogs/askcore/alternate-data-streams-in-ntfs">Alternate
     * Data Streams</a>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String ext1 = Files.getFileExtension("document.pdf");
     * // Returns: "pdf"
     * 
     * String ext2 = Files.getFileExtension("archive.tar.gz");
     * // Returns: "gz"
     * 
     * String ext3 = Files.getFileExtension("README");
     * // Returns: ""
     * }</pre>
     *
     * @param fullName the file name to extract the extension from
     * @return the file extension (without the dot), or empty string if none
     */
    public static String getFileExtension(final String fullName) {
        return com.google.common.io.Files.getFileExtension(fullName);
    }

    /**
     * Returns the <a href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> for
     * the file at the given path, or the empty string if the file has no extension. The result does
     * not include the '{@code .}'.
     *
     * <p><b>Note:</b> This method simply returns everything after the last '{@code .}' in the file's
     * name as determined by {@link Path#getFileName}. It does not account for any filesystem-specific
     * behavior that the {@link Path} API does not already account for. For example, on NTFS it will
     * report {@code "txt"} as the extension for the filename {@code "foo.exe:.txt"} even though NTFS
     * will drop the {@code ":.txt"} part of the name when the file is actually created on the
     * filesystem due to NTFS's
     * <a href="https://learn.microsoft.com/en-us/archive/blogs/askcore/alternate-data-streams-in-ntfs">Alternate
     * Data Streams</a>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("/home/user/document.pdf");
     * String ext = Files.getFileExtension(path);
     * // Returns: "pdf"
     * }</pre>
     *
     * @param path the path whose file extension should be extracted
     * @return the file extension (without the dot), or empty string if none
     */
    public static String getFileExtension(final Path path) {
        return com.google.common.io.MoreFiles.getFileExtension(path);
    }

    /**
     * Returns a {@link Traverser} instance for the file and directory tree. The returned traverser
     * starts from a {@link File} and will return all files and directories it encounters.
     *
     * <p><b>Warning:</b> {@code File} provides no support for symbolic links, and as such there is no
     * way to ensure that a symbolic link to a directory is not followed when traversing the tree. In
     * this case, iterables created by this traverser could contain files that are outside the
     * given directory or even be infinite if there is a symbolic link loop.
     *
     * <p>If available, consider using {@link MoreFiles#fileTraverser()} instead. It behaves the same
     * except that it doesn't follow symbolic links and returns {@code Path} instances.
     *
     * <p>If the {@link File} passed to one of the {@link Traverser} methods does not exist or is not
     * a directory, no exception will be thrown and the returned {@link Iterable} will contain a
     * single element: that file.
     *
     * <p><b>Usage Examples:</b></p> {@code Files.fileTraverser().depthFirstPreOrder(new File("/"))} may return files
     * with the following paths: {@code ["/", "/etc", "/etc/config.txt", "/etc/fonts", "/home",
     * "/home/alice", ...]}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Traverser<File> traverser = Files.fileTraverser();
     * for (File file : traverser.breadthFirst(new File("/home/user"))) {
     *     System.out.println(file.getPath());
     * }
     * }</pre>
     *
     * @return a traverser for traversing file trees starting from File objects
     */
    public static Traverser<File> fileTraverser() {
        return com.google.common.io.Files.fileTraverser();
    }

    /**
     * Returns a {@link Traverser} instance for the file and directory tree. The returned traverser
     * starts from a {@link Path} and will return all files and directories it encounters.
     *
     * <p>The returned traverser attempts to avoid following symbolic links to directories. However,
     * the traverser cannot guarantee that it will not follow symbolic links to directories as it is
     * possible for a directory to be replaced with a symbolic link between checking if the file is a
     * directory and actually reading the contents of that directory.
     *
     * <p>If the {@link Path} passed to one of the traversal methods does not exist or is not a
     * directory, no exception will be thrown and the returned {@link Iterable} will contain a single
     * element: that path.
     *
     * <p>{@link DirectoryIteratorException} may be thrown when iterating {@link Iterable} instances
     * created by this traverser if an {@link IOException} is thrown by a call to {@link
     * #listFiles(Path)}.
     *
     * <p><b>Usage Examples:</b></p> {@code MoreFiles.fileTraverser().depthFirstPreOrder(Paths.get("/"))} may return the
     * following paths: {@code ["/", "/etc", "/etc/config.txt", "/etc/fonts", "/home", "/home/alice",
     * ...]}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Traverser<Path> traverser = Files.pathTraverser();
     * for (Path path : traverser.breadthFirst(Paths.get("/home/user"))) {
     *     System.out.println(path);
     * }
     * }</pre>
     *
     * @return a traverser for traversing file trees starting from Path objects
     */
    public static Traverser<Path> pathTraverser() {
        return com.google.common.io.MoreFiles.fileTraverser();
    }

    /**
     * Returns an immutable list of paths to the files contained in the given directory.
     * The returned list does not include the directory itself or any subdirectories.
     * Hidden files are included in the results.
     *
     * <p>The order of the returned files is not specified and may vary between invocations
     * or platforms.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<Path> files = Files.listFiles(Paths.get("/home/user"));
     * for (Path file : files) {
     *     System.out.println(file.getFileName());
     * }
     * }</pre>
     *
     * @param dir the directory whose files should be listed
     * @return an immutable list of paths to files in the directory
     * @throws NoSuchFileException if the file does not exist <i>(optional specific exception)</i>
     * @throws NotDirectoryException if the file could not be opened because it is not a directory
     *     <i>(optional specific exception)</i>
     * @throws IOException if an I/O error occurs
     * @see IOUtil#listFiles(File)
     */
    public static ImmutableList<Path> listFiles(final Path dir) throws IOException {
        return ImmutableList.wrap(com.google.common.io.MoreFiles.listFiles(dir));
    }

    /**
     * Deletes the file or directory at the given {@code path} recursively. Deletes symbolic links,
     * not their targets (subject to the caveat below).
     *
     * <p>If an I/O exception occurs attempting to read, open or delete any file under the given
     * directory, this method skips that file and continues. All such exceptions are collected and,
     * after attempting to delete all files, an {@code IOException} is thrown containing those
     * exceptions as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
     *
     * <p><b>Warning: Security of recursive deletes</b>
     *
     * <p>On a file system that supports symbolic links and does <i>not</i> support {@link
     * SecureDirectoryStream}, it is possible for a recursive delete to delete files and directories
     * that are <i>outside</i> the directory being deleted. This can happen if, after checking that a
     * file is a directory (and not a symbolic link), that directory is replaced by a symbolic link to
     * an outside directory before the call that opens the directory to read its entries.
     *
     * <p>By default, this method throws {@link InsecureRecursiveDeleteException} if it can't
     * guarantee the security of recursive deletes. If you wish to allow the recursive deletes anyway,
     * pass {@link RecursiveDeleteOption#ALLOW_INSECURE} to this method to override that behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Safe recursive delete
     * Files.deleteRecursively(Paths.get("/tmp/old_data"));
     * 
     * // Force recursive delete even on insecure file systems
     * Files.deleteRecursively(Paths.get("/tmp/old_data"), RecursiveDeleteOption.ALLOW_INSECURE);
     * }</pre>
     *
     * @param path the path to delete (file or directory)
     * @param options optional delete options, such as {@link RecursiveDeleteOption#ALLOW_INSECURE}
     * @throws NoSuchFileException if {@code path} does not exist <i>(optional specific exception)</i>
     * @throws InsecureRecursiveDeleteException if the security of recursive deletes can't be
     *     guaranteed for the file system and {@link RecursiveDeleteOption#ALLOW_INSECURE} was not
     *     specified
     * @throws IOException if {@code path} or any file in the subtree rooted at it can't be deleted
     *     for any reason
     */
    public static void deleteRecursively(final Path path, final RecursiveDeleteOption... options) throws IOException {
        com.google.common.io.MoreFiles.deleteRecursively(path, options);
    }

    /**
     * Deletes all files within the directory at the given {@code path} {@linkplain #deleteRecursively
     * recursively}. Does not delete the directory itself. Deletes symbolic links, not their targets
     * (subject to the caveat below). If {@code path} itself is a symbolic link to a directory, that
     * link is followed and the contents of the directory it targets are deleted.
     *
     * <p>If an I/O exception occurs attempting to read, open or delete any file under the given
     * directory, this method skips that file and continues. All such exceptions are collected and,
     * after attempting to delete all files, an {@code IOException} is thrown containing those
     * exceptions as {@linkplain Throwable#getSuppressed() suppressed exceptions}.
     *
     * <p><b>Warning: Security of recursive deletes</b>
     *
     * <p>On a file system that supports symbolic links and does <i>not</i> support {@link
     * SecureDirectoryStream}, it is possible for a recursive delete to delete files and directories
     * that are <i>outside</i> the directory being deleted. This can happen if, after checking that a
     * file is a directory (and not a symbolic link), that directory is replaced by a symbolic link to
     * an outside directory before the call that opens the directory to read its entries.
     *
     * <p>By default, this method throws {@link InsecureRecursiveDeleteException} if it can't
     * guarantee the security of recursive deletes. If you wish to allow the recursive deletes anyway,
     * pass {@link RecursiveDeleteOption#ALLOW_INSECURE} to this method to override that behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Empty a directory but keep the directory itself
     * Path tempDir = Paths.get("/tmp/working");
     * Files.deleteDirectoryContents(tempDir);
     * // tempDir still exists but is now empty
     * }</pre>
     *
     * @param path the directory whose contents should be deleted
     * @param options optional delete options, such as {@link RecursiveDeleteOption#ALLOW_INSECURE}
     * @throws NoSuchFileException if {@code path} does not exist <i>(optional specific exception)</i>
     * @throws NotDirectoryException if the file at {@code path} is not a directory <i>(optional
     *     specific exception)</i>
     * @throws InsecureRecursiveDeleteException if the security of recursive deletes can't be
     *     guaranteed for the file system and {@link RecursiveDeleteOption#ALLOW_INSECURE} was not
     *     specified
     * @throws IOException if one or more files can't be deleted for any reason
     */
    public static void deleteDirectoryContents(final Path path, final RecursiveDeleteOption... options) throws IOException {
        com.google.common.io.MoreFiles.deleteDirectoryContents(path, options);
    }

    /**
     * Returns a predicate that returns the result of {@link java.nio.file.Files#isDirectory(Path,
     * LinkOption...)} on input paths with the given link options. This is useful for filtering
     * collections of paths.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Path> directories = paths.stream()
     *     .filter(Files.isDirectory())
     *     .collect(Collectors.toList());
     * 
     * // Don't follow symbolic links
     * Predicate<Path> isRealDir = Files.isDirectory(LinkOption.NOFOLLOW_LINKS);
     * }</pre>
     *
     * @param options link options to use when checking if a path is a directory
     * @return a predicate that tests if a path is a directory
     */
    public static Predicate<Path> isDirectory(final LinkOption... options) {
        return com.google.common.io.MoreFiles.isDirectory(options);
    }

    /**
     * Returns a predicate that returns the result of {@link java.nio.file.Files#isRegularFile(Path,
     * LinkOption...)} on input paths with the given link options. This is useful for filtering
     * collections of paths.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Path> regularFiles = paths.stream()
     *     .filter(Files.isRegularFile())
     *     .collect(Collectors.toList());
     * 
     * // Don't follow symbolic links
     * Predicate<Path> isRealFile = Files.isRegularFile(LinkOption.NOFOLLOW_LINKS);
     * }</pre>
     *
     * @param options link options to use when checking if a path is a regular file
     * @return a predicate that tests if a path is a regular file
     */
    public static Predicate<Path> isRegularFile(final LinkOption... options) {
        return com.google.common.io.MoreFiles.isRegularFile(options);
    }

    /**
     * Reads all bytes from a file into a byte array. The method ensures that the file is closed when
     * all bytes have been read or an I/O error, or other runtime exception, is thrown.
     *
     * <p>This method is intended for simple cases where it is convenient to read all bytes
     * into a byte array. It is not intended for reading large files.
     *
     * <p>This method is equivalent to: {@link java.nio.file.Files#readAllBytes(Path)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = Files.readAllBytes(new File("image.jpg"));
     * }</pre>
     *
     * @param   file the file to read from 
     * @return  a byte array containing the content read from the file 
     * @throws  IOException
     *          if an I/O error occurs reading from the file
     * @throws  OutOfMemoryError
     *          if the file is extremely large, for example larger than {@code 2GB}
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is installed, the {@link
     *          SecurityManager#checkRead(String) checkRead} method is invoked to check read access to
     *          the file.
     * @see #readString(File)
     * @see java.nio.file.Files#readAllBytes(Path)
     * @see IOUtil#readAllBytes(File)
     */
    public static byte[] readAllBytes(final File file) throws IOException {
        return java.nio.file.Files.readAllBytes(file.toPath());
    }

    /**
     * Reads all content from a file into a string using UTF-8 charset. The method ensures
     * that the file is closed when all bytes have been read or an I/O error, or other runtime
     * exception, is thrown.
     *
     * <p>This method is intended for simple cases where it is convenient to read all content
     * into a string. It is not intended for reading large files.
     *
     * <p>This method is equivalent to: {@code readString(file, StandardCharsets.UTF_8)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String content = Files.readString(new File("config.json"));
     * }</pre>
     *
     * @param file the file to read from
     * @return a string containing the content read from the file
     * @throws IOException if an I/O error occurs reading from the file
     * @throws OutOfMemoryError if the file is extremely large, for example larger than {@code 2GB}
     * @throws SecurityException In the case of the default provider, and a security manager is
     *         installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked
     * @see #readString(File, Charset)
     * @see java.nio.file.Files#readString(Path)
     * @see IOUtil#readAllToString(File)
     */
    public static String readString(final File file) throws IOException {
        return readString(file, StandardCharsets.UTF_8);
    }

    /**
     * Reads all bytes from a file into a string using the given character set. The method ensures
     * that the file is closed when all bytes have been read or an I/O error, or other runtime
     * exception, is thrown.
     *
     * <p>This method is intended for simple cases where it is convenient to read all content
     * into a string. It is not intended for reading large files.
     *
     * <p>This method is equivalent to: {@link java.nio.file.Files#readString(Path, Charset)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String content = Files.readString(new File("document.txt"), StandardCharsets.ISO_8859_1);
     * }</pre>
     *
     * @param file the file to read from
     * @param cs the character set used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return a string containing the content read from the file
     * @throws IOException if an I/O error occurs reading from the file
     * @throws OutOfMemoryError if the file is extremely large, for example, larger than {@code 2GB}
     * @throws SecurityException In the case of the default provider, and a security manager is
     *         installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked
     * @see java.nio.file.Files#readString(Path, Charset)
     * @see IOUtil#readAllToString(File, Charset)
     */
    public static String readString(final File file, Charset cs) throws IOException {
        return java.nio.file.Files.readString(file.toPath(), cs);
    }

    /**
     * Reads all lines from a file using UTF-8 charset. The lines do not include line-termination characters, but do include
     * other leading and trailing whitespace.
     *
     * <p>This method returns a mutable {@code List}. For an {@code ImmutableList}, use {@code
     * Files.asCharSource(file, charset).readLines()}.
     *
     * <p>This method is intended for simple cases where it is convenient to read all lines
     * in a single operation. It is not intended for reading large files.
     *
     * <p>This method is equivalent to: {@code readAllLines(file, StandardCharsets.UTF_8)}.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#readAllLines(java.nio.file.Path, Charset)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> lines = Files.readAllLines(new File("data.txt"));
     * for (String line : lines) {
     *     processLine(line);
     * }
     * }</pre>
     *
     * @param file the file to read from
     * @return a mutable {@link List} containing all the lines
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     *         installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked
     * @see #readAllLines(File, Charset)
     * @see java.nio.file.Files#readAllLines(Path, Charset)
     * @see IOUtil#readAllLines(File)        */
    public static List<String> readAllLines(final File file) throws IOException {
        return readAllLines(file, StandardCharsets.UTF_8);
    }

    /**
     * Reads all lines from a file using the given character set. The lines do not include line-termination
     * characters, but do include other leading and trailing whitespace.
     *
     * <p>This method returns a mutable {@code List}. For an {@code ImmutableList}, use {@code
     * Files.asCharSource(file, charset).readLines()}.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#readAllLines(java.nio.file.Path, Charset)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read all lines using a specific charset
     * List<String> lines = Files.readAllLines(new File("data.csv"), StandardCharsets.ISO_8859_1);
     *
     * // Process each line
     * for (String line : lines) {
     *     String[] fields = line.split(",");
     *     // Process fields
     * }
     * }</pre>
     *
     * @param file the file to read from
     * @param cs the character set used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return a mutable {@link List} containing all the lines
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     *         installed, the {@link SecurityManager#checkRead(String) checkRead} method is invoked
     *         to check read access to the file
     * @see java.nio.file.Files#readAllLines(Path, Charset)
     * @see IOUtil#readAllLines(File, Charset)
     */
    public static List<String> readAllLines(final File file, Charset cs) throws IOException {
        return java.nio.file.Files.readAllLines(file.toPath(), cs);
    }

    /**
     * The Class MoreFiles.
     */
    public static final class MoreFiles extends Files {

        private MoreFiles() {
            // singleton
        }
    }
}
