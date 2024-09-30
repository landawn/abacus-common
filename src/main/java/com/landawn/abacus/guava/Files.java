/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.graph.Traverser;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.RecursiveDeleteOption;
import com.landawn.abacus.util.ImmutableList;

/**
 * Copied from Google Guava under Apache License v2.
 * The purpose is to provide unified APIs.
 *
 * @see java.nio.file.Files
 * @see com.google.common.io.Files
 * @see com.landawn.abacus.util.IOUtil
 * @see com.landawn.abacus.util.Strings
 *
 */
public abstract class Files { //NOSONAR

    private Files() {
        // singleton
    }

    /**
     *
     * @param file
     * @param charset
     * @return
     * @throws FileNotFoundException the file not found exception
     */
    public static BufferedReader newReader(final File file, final Charset charset) throws FileNotFoundException {
        return com.google.common.io.Files.newReader(file, charset);
    }

    /**
     *
     * @param file
     * @param charset
     * @return
     * @throws FileNotFoundException the file not found exception
     */
    public static BufferedWriter newWriter(final File file, final Charset charset) throws FileNotFoundException {
        return com.google.common.io.Files.newWriter(file, charset);
    }

    /**
     * As byte source.
     *
     * @param file
     * @return
     */
    public static ByteSource asByteSource(final File file) {
        return com.google.common.io.Files.asByteSource(file);
    }

    /**
     * As byte source.
     *
     * @param path
     * @param options
     * @return
     */
    @SafeVarargs
    public static ByteSource asByteSource(final Path path, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asByteSource(path, options);
    }

    /**
     * As byte sink.
     *
     * @param file
     * @param modes
     * @return
     */
    @SafeVarargs
    public static ByteSink asByteSink(final File file, final FileWriteMode... modes) {
        return com.google.common.io.Files.asByteSink(file, modes);
    }

    /**
     * As byte sink.
     *
     * @param path
     * @param options
     * @return
     */
    @SafeVarargs
    public static ByteSink asByteSink(final Path path, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asByteSink(path, options);
    }

    /**
     * As char source.
     *
     * @param file
     * @param charset
     * @return
     */
    public static CharSource asCharSource(final File file, final Charset charset) {
        return com.google.common.io.Files.asCharSource(file, charset);
    }

    /**
     * As char source.
     *
     * @param path
     * @param charset
     * @param options
     * @return
     */
    @SafeVarargs
    public static CharSource asCharSource(final Path path, final Charset charset, final OpenOption... options) {
        return com.google.common.io.MoreFiles.asCharSource(path, charset, options);
    }

    /**
     * As char sink.
     *
     * @param file
     * @param charset
     * @param modes
     * @return
     */
    @SafeVarargs
    public static CharSink asCharSink(final File file, final Charset charset, final FileWriteMode... modes) {
        return com.google.common.io.Files.asCharSink(file, charset, modes);
    }

    /**
     * As char sink.
     *
     * @param path
     * @param charset
     * @param options
     * @return
     */
    @SafeVarargs
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
     *
     * @param from
     * @param to
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void write(final byte[] from, final File to) throws IOException {
        com.google.common.io.Files.write(from, to);
    }

    /**
     *
     * @param file1
     * @param file2
     * @return true, if successful
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static boolean equal(final File file1, final File file2) throws IOException { //NOSONAR
        return com.google.common.io.Files.equal(file1, file2);
    }

    /**
     *
     * @param path1
     * @param path2
     * @return true, if successful
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static boolean equal(final Path path1, final Path path2) throws IOException { //NOSONAR
        return com.google.common.io.MoreFiles.equal(path1, path2);
    }

    /**
     *
     * @param file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void touch(final File file) throws IOException {
        com.google.common.io.Files.touch(file);
    }

    /**
     *
     * @param path
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void touch(final Path path) throws IOException {
        com.google.common.io.MoreFiles.touch(path);
    }

    /**
     *
     * @return
     * @deprecated For Android users, see the <a
     *     href="https://developer.android.com/training/data-storage" target="_blank">Data and File
     *     Storage overview</a> to select an appropriate temporary directory (perhaps {@code
     *     context.getCacheDir()}). For developers on Java 7 or later, use {@link
     *     java.nio.file.Files#createTempDirectory}, transforming it to a {@link File} using {@link
     *     java.nio.file.Path#toFile() toFile()} if needed.
     */
    @Deprecated
    public static File createTempDir() {
        return com.google.common.io.Files.createTempDir(); //NOSONAR
    }

    /**
     * Creates the parent dirs.
     *
     * @param file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void createParentDirs(final File file) throws IOException {
        com.google.common.io.Files.createParentDirs(file);
    }

    /**
     * Creates the parent directories.
     *
     * @param path
     * @param attrs
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SafeVarargs
    public static void createParentDirectories(final Path path, final FileAttribute<?>... attrs) throws IOException {
        com.google.common.io.MoreFiles.createParentDirectories(path, attrs);
    }

    /**
     *
     * @param from
     * @param to
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copy(final File from, final OutputStream to) throws IOException {
        com.google.common.io.Files.copy(from, to);
    }

    /**
     *
     * @param from
     * @param to
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copy(final File from, final File to) throws IOException {
        com.google.common.io.Files.copy(from, to);
    }

    /**
     *
     * @param from
     * @param to
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void move(final File from, final File to) throws IOException {
        com.google.common.io.Files.move(from, to);
    }

    /**
     *
     * @param file
     * @param charset
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static List<String> readLines(final File file, final Charset charset) throws IOException {
        return com.google.common.io.Files.readLines(file, charset);
    }

    /**
     *
     * @param file
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static MappedByteBuffer map(final File file) throws IOException {
        return com.google.common.io.Files.map(file);
    }

    /**
     *
     * @param file
     * @param mode
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static MappedByteBuffer map(final File file, final MapMode mode) throws IOException {
        return com.google.common.io.Files.map(file, mode);
    }

    /**
     *
     * @param file
     * @param mode
     * @param size
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static MappedByteBuffer map(final File file, final MapMode mode, final long size) throws IOException {
        return com.google.common.io.Files.map(file, mode, size);
    }

    /**
     *
     * @param pathname
     * @return
     */
    public static String simplifyPath(final String pathname) {
        return com.google.common.io.Files.simplifyPath(pathname);
    }

    /**
     * Gets the name without extension.
     *
     * @param file
     * @return
     */
    public static String getNameWithoutExtension(final String file) {
        return com.google.common.io.Files.getNameWithoutExtension(file);
    }

    /**
     * Gets the name without extension.
     *
     * @param path
     * @return
     */
    public static String getNameWithoutExtension(final Path path) {
        return com.google.common.io.MoreFiles.getNameWithoutExtension(path);
    }

    /**
     * Gets the file extension.
     *
     * @param fullName
     * @return
     */
    public static String getFileExtension(final String fullName) {
        return com.google.common.io.Files.getFileExtension(fullName);
    }

    /**
     * Gets the file extension.
     *
     * @param path
     * @return
     */
    public static String getFileExtension(final Path path) {
        return com.google.common.io.MoreFiles.getFileExtension(path);
    }

    /**
     * Returns a {@link Traverser} instance for the file and directory tree. The returned traverser
     * starts from a {@link Path} and will return all files and directories it encounters.
     *
     * @return
     * @see com.google.common.io.MoreFiles#fileTraverser()
     */
    public static Traverser<File> fileTraverser() {
        return com.google.common.io.Files.fileTraverser();
    }

    /**
     * Returns a {@link Traverser} instance for the file and directory tree. The returned traverser
     * starts from a {@link Path} and will return all files and directories it encounters.
     *
     *
     * @return
     * @see com.google.common.io.MoreFiles#pathTraverser()
     */
    public static Traverser<Path> pathTraverser() {
        return com.google.common.io.MoreFiles.fileTraverser();
    }

    /**
     *
     * @param dir
     * @return
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static ImmutableList<Path> listFiles(final Path dir) throws IOException {
        return ImmutableList.wrap(com.google.common.io.MoreFiles.listFiles(dir));
    }

    /**
     * Deletes the file or directory at the given {@code path} recursively. Deletes symbolic links,
     * not their targets (subject to the caveat below).
     *
     * @param path
     * @param options
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SafeVarargs
    public static void deleteRecursively(final Path path, final RecursiveDeleteOption... options) throws IOException {
        com.google.common.io.MoreFiles.deleteRecursively(path, options);
    }

    /**
     * Delete directory contents.
     *
     * @param path
     * @param options
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @SafeVarargs
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
