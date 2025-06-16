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
 * Copied from Google Guava under Apache License v2.
 * The purpose is to provide unified APIs.
 *
 * @see java.nio.file.Files
 * @see com.google.common.io.Files
 * @see com.google.common.io.MoreFiles
 * @see com.landawn.abacus.util.IOUtil
 * @see com.landawn.abacus.util.Strings
 *
 */
public abstract class Files { //NOSONAR

    private Files() {
        // singleton
    }

    /**
     * Returns a buffered reader that reads from a file using the given character set.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#newBufferedReader(java.nio.file.Path, Charset)}.
     *
     * @param file the file to read from
     * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return the buffered reader
     */
    public static BufferedReader newReader(final File file, final Charset charset) throws FileNotFoundException {
        return com.google.common.io.Files.newReader(file, charset);
    }

    /**
     * Returns a buffered writer that writes to a file using the given character set.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#newBufferedWriter(java.nio.file.Path, Charset,
     * java.nio.file.OpenOption...)}.
     *
     * @param file the file to write to
     * @param charset the charset used to encode the output stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return the buffered writer
     */
    public static BufferedWriter newWriter(final File file, final Charset charset) throws FileNotFoundException {
        return com.google.common.io.Files.newWriter(file, charset);
    }

    /**
     * Returns a new {@link ByteSource} for reading bytes from the given file.
     *
     */
    public static ByteSource asByteSource(final File file) {
        return com.google.common.io.Files.asByteSource(file);
    }

    /**
     * Returns a view of the given {@code path} as a {@link ByteSource}.
     *
     * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
     * and may affect the behavior of the returned source and the streams it provides. See {@link
     * StandardOpenOption} for the standard options that may be provided. Providing no options is
     * equivalent to providing the {@link StandardOpenOption#READ READ} option.
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
     */
    public static ByteSink asByteSink(final File file, final FileWriteMode... modes) {
        return com.google.common.io.Files.asByteSink(file, modes);
    }

    /**
     * Returns a view of the given {@code path} as a {@link ByteSink}.
     *
     * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
     * and may affect the behavior of the returned sink and the streams it provides. See {@link
     * StandardOpenOption} for the standard options that may be provided. Providing no options is
     * equivalent to providing the {@link StandardOpenOption#CREATE CREATE}, {@link
     * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} and {@link StandardOpenOption#WRITE
     * WRITE} options.
     */
    public static ByteSink asByteSink(final Path path, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asByteSink(path, options);
    }

    /**
     * Returns a new {@link CharSource} for reading character data from the given file using the given
     * character set.
     *
     */
    public static CharSource asCharSource(final File file, final Charset charset) {
        return com.google.common.io.Files.asCharSource(file, charset);
    }

    /**
     * Returns a view of the given {@code path} as a {@link CharSource} using the given {@code
     * charset}.
     *
     * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
     * and may affect the behavior of the returned source and the streams it provides. See {@link
     * StandardOpenOption} for the standard options that may be provided. Providing no options is
     * equivalent to providing the {@link StandardOpenOption#READ READ} option.
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
     */
    public static CharSink asCharSink(final File file, final Charset charset, final FileWriteMode... modes) {
        return com.google.common.io.Files.asCharSink(file, charset, modes);
    }

    /**
     * Returns a view of the given {@code path} as a {@link CharSink} using the given {@code charset}.
     *
     * <p>Any {@linkplain OpenOption open options} provided are used when opening streams to the file
     * and may affect the behavior of the returned sink and the streams it provides. See {@link
     * StandardOpenOption} for the standard options that may be provided. Providing no options is
     * equivalent to providing the {@link StandardOpenOption#CREATE CREATE}, {@link
     * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} and {@link StandardOpenOption#WRITE
     * WRITE} options.
     */
    public static CharSink asCharSink(final Path path, final Charset charset, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asCharSink(path, charset, options);
    }

    /**
     * To byte array.
     *
     * @param file
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static byte[] toByteArray(final File file) throws IOException {
        return com.google.common.io.Files.toByteArray(file);
    }

    /**
     * Overwrites a file with the contents of a byte array.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#write(java.nio.file.Path, byte[], java.nio.file.OpenOption...)}.
     *
     * @param from the bytes to write
     * @param to the destination file
     * @throws IOException if an I/O error occurs
     */
    public static void write(final byte[] from, final File to) throws IOException {
        com.google.common.io.Files.write(from, to);
    }

    /**
     * Returns {@code true} if the given files exist, are not directories, and contain the same bytes.
     *
     * @throws IOException if an I/O error occurs
     */
    public static boolean equal(final File file1, final File file2) throws IOException { //NOSONAR
        return com.google.common.io.Files.equal(file1, file2);
    }

    /**
     * Returns {@code true} if the files located by the given paths exist, are not directories, and contain
     * the same bytes.
     *
     * @throws IOException if an I/O error occurs
     */
    public static boolean equal(final Path path1, final Path path2) throws IOException { //NOSONAR
        return com.google.common.io.MoreFiles.equal(path1, path2);
    }

    /**
     * Creates an empty file or updates the last updated timestamp on the same as the unix command of
     * the same name.
     *
     * @param file the file to create or update
     * @throws IOException if an I/O error occurs
     */
    public static void touch(final File file) throws IOException {
        com.google.common.io.Files.touch(file);
    }

    /**
     * Like the unix command of the same name, creates an empty file or updates the last modified
     * timestamp of the existing file at the given path to the current system time.
     *
     * @param path
     * @throws IOException Signals that an I/O exception has occurred.
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
     * Previous versions would create a directory that is more accessible, as discussed in <a
     * href="https://github.com/google/guava/issues/4011">CVE-2020-8908</a>.)
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
     * @return the newly created directory
     * @throws IllegalStateException if the directory could not be created, such as if the system does
     *     not support creating temporary directories securely
     * @deprecated For Android users, see the <a
     *     href="https://developer.android.com/training/data-storage" target="_blank">Data and File
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
     * @throws IOException if an I/O error occurs, or if any necessary but nonexistent parent
     *     directories of the specified file could not be created.
     */
    public static void createParentDirs(final File file) throws IOException {
        com.google.common.io.Files.createParentDirs(file);
    }

    /**
     * Creates any necessary but nonexistent parent directories of the specified path. Note that if
     * this operation fails, it may have succeeded in creating some (but not all) of the necessary
     * parent directories. The parent directory is created with the given {@code attrs}.
     *
     * @throws IOException if an I/O error occurs, or if any necessary but nonexistent parent
     *     directories of the specified file could not be created.
     */
    public static void createParentDirectories(final Path path, final FileAttribute<?>... attrs) throws IOException {
        com.google.common.io.MoreFiles.createParentDirectories(path, attrs);
    }

    /**
     * Copies all bytes from a file to an output stream.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#copy(java.nio.file.Path, OutputStream)}.
     *
     * @param from the source file
     * @param to the output stream
     * @throws IOException if an I/O error occurs
     */
    public static void copy(final File from, final OutputStream to) throws IOException {
        com.google.common.io.Files.copy(from, to);
    }

    /**
     * Copies all the bytes from one file to another.
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
     * @param from the source file
     * @param to the destination file
     * @throws IOException if an I/O error occurs
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
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link java.nio.file.Files#move}.
     *
     * @param from the source file
     * @param to the destination file
     * @throws IOException if an I/O error occurs
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
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#readAllLines(java.nio.file.Path, Charset)}.
     *
     * @param file the file to read from
     * @param charset the charset used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return a mutable {@link List} containing all the lines
     * @throws IOException if an I/O error occurs
     */
    public static List<String> readLines(final File file, final Charset charset) throws IOException {
        return com.google.common.io.Files.readLines(file, charset);
    }

    /**
     * Fully maps a file read-only in to memory as per {@link
     * FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}.
     *
     * <p>Files are mapped from offset 0 to its length.
     *
     * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
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
     * MapMode}.
     *
     * <p>Files are mapped from offset 0 to its length.
     *
     * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file}
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
     * long, long)} using the requested {@link MapMode}.
     *
     * <p>Files are mapped from offset 0 to {@code size}.
     *
     * <p>If the mode is {@link MapMode#READ_WRITE} and the file does not exist, it will be created
     * with the requested {@code size}. Thus, this method is useful for creating memory mapped files
     * which do not yet exist.
     *
     * <p>This only works for files ≤ {@link Integer#MAX_VALUE} bytes.
     *
     * @param file the file to map
     * @param mode the mode to use when mapping {@code file}
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
     */
    public static String simplifyPath(final String pathname) {
        return com.google.common.io.Files.simplifyPath(pathname);
    }

    /**
     * Returns the file name without its <a
     * href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
     * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
     *
     * @param file The name of the file to trim the extension from. This can be either a fully
     *     qualified file name (including a path) or just a file name.
     * @return The file name without its path or extension.
     */
    public static String getNameWithoutExtension(final String file) {
        return com.google.common.io.Files.getNameWithoutExtension(file);
    }

    /**
     * Returns the file name without its <a
     * href="http://en.wikipedia.org/wiki/Filename_extension">file extension</a> or path. This is
     * similar to the {@code basename} unix command. The result does not include the '{@code .}'.
     *
     * @param path
     * @return
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
     * filesystem due to NTFS's <a
     * href="https://learn.microsoft.com/en-us/archive/blogs/askcore/alternate-data-streams-in-ntfs">Alternate
     * Data Streams</a>.
     *
     * @param fullName
     * @return
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
     * filesystem due to NTFS's <a
     * href="https://learn.microsoft.com/en-us/archive/blogs/askcore/alternate-data-streams-in-ntfs">Alternate
     * Data Streams</a>.
     *
     * @param path
     * @return
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
     * <p>Example: {@code Files.fileTraverser().depthFirstPreOrder(new File("/"))} may return files
     * with the following paths: {@code ["/", "/etc", "/etc/config.txt", "/etc/fonts", "/home",
     * "/home/alice", ...]}
     *
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
     * <p>Example: {@code MoreFiles.fileTraverser().depthFirstPreOrder(Paths.get("/"))} may return the
     * following paths: {@code ["/", "/etc", "/etc/config.txt", "/etc/fonts", "/home", "/home/alice",
     * ...]}
     *
     */
    public static Traverser<Path> pathTraverser() {
        return com.google.common.io.MoreFiles.fileTraverser();
    }

    /**
     * Returns an immutable list of paths to the files contained in the given directory.
     *
     * @param dir
     * @return
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
     * <h2>Warning: Security of recursive deletes</h2>
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
     * @param path
     * @param options
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
     * <h2>Warning: Security of recursive deletes</h2>
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
     * @param path
     * @param options
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
     * LinkOption...)} on input paths with the given link options.
     *
     * @param options
     * @return
     */
    public static Predicate<Path> isDirectory(final LinkOption... options) {
        return com.google.common.io.MoreFiles.isDirectory(options);
    }

    /**
     * Returns a predicate that returns the result of {@link java.nio.file.Files#isRegularFile(Path,
     * LinkOption...)} on input paths with the given link options.
     *
     * @param options
     * @return
     */
    public static Predicate<Path> isRegularFile(final LinkOption... options) {
        return com.google.common.io.MoreFiles.isRegularFile(options);
    }

    /**
     * Reads all bytes from a file into a byte array. The method ensures that the file is closed when
     * all bytes have been read or an I/O error, or other runtime exception, is thrown.
     *
     * <p> This method is equivalent to: {@link readAllBytes(Path) readAllBytes(path)}.
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
     * Reads all bytes from a file into a byte array using the given character set. The method ensures
     * that the file is closed when all bytes have been read or an I/O error, or other runtime
     * exception, is thrown.
     *
     * <p>This method is equivalent to: {@link readAllBytes(Path, Charset) readAllBytes(path, cs)}.
     *
     * @param file the file to read from
     * @param cs the character set used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return a byte array containing the content read from the file
     * @throws IOException if an I/O error occurs reading from the file
     * @throws OutOfMemoryError if the file is extremely large, for example larger than {@code 2GB}
     * @throws SecurityException In the case of the default provider, and a security manager is
     * @see #readString(File)
     * @see java.nio.file.Files#readAllBytes(Path, Charset)
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
     * <p>This method is equivalent to: {@link readString(Path, Charset) readString(path, cs)}.
     *
     * @param file the file to read from
     * @param cs the character set used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return a string containing the content read from the file
     * @throws IOException if an I/O error occurs reading from the file
     * @throws OutOfMemoryError if the file is extremely large, for example larger than {@code 2GB}
     * @throws SecurityException In the case of the default provider, and a security manager is
     * @see java.nio.file.Files#readString(Path, Charset)
     * @see IOUtil#readAllToString(File, Charset)
     */
    public static String readString(final File file, Charset cs) throws IOException {
        return java.nio.file.Files.readString(file.toPath(), cs);
    }

    /**
     * Reads all lines from a file. The lines do not include line-termination characters, but do include
     * other leading and trailing whitespace.
     *
     * <p>This method returns a mutable {@code List}. For an {@code ImmutableList}, use {@code
     * Files.asCharSource(file, charset).readLines()}.
     *
     * <p><b>{@link java.nio.file.Path} equivalent:</b> {@link
     * java.nio.file.Files#readAllLines(java.nio.file.Path, Charset)}.
     *
     * @param file the file to read from
     * @return a mutable {@link List} containing all the lines
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
     * @see #readAllLines(File, Charset)
     * @see java.nio.file.Files#readAllLines(Path, Charset)
     * @see IOUtil#readAllLines(File)
     */
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
     * @param file the file to read from
     * @param cs the character set used to decode the input stream; see {@link StandardCharsets} for
     *     helpful predefined constants
     * @return a mutable {@link List} containing all the lines
     * @throws IOException if an I/O error occurs
     * @throws SecurityException In the case of the default provider, and a security manager is
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

        /**
         * Instantiates a new more files.
         */
        private MoreFiles() {
            // singleton
        }
    }
}
