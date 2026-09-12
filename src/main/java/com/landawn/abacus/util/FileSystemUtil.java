/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

/**
 * General File System utilities for checking disk space.
 *
 * <p>This class is copied from Apache Commons IO developed at The Apache Software Foundation
 * under the Apache License 2.0.</p>
 *
 * <p>This class provides static utility methods for querying file system
 * free space by invoking native operating system commands. It supports
 * Windows, Unix, and POSIX-compliant systems.</p>
 * <p>Paths are interpreted literally: environment-variable syntax in a Windows path is not expanded,
 * and relative Unix paths are made absolute before invoking the command.</p>
 *
 * <p>The class works by executing platform-specific commands:</p>
 * <ul>
 * <li>Windows: uses 'dir /a /-c' command</li>
 * <li>Unix/Linux/macOS: uses 'df -k' command</li>
 * <li>AIX/HP-UX: uses 'df -kP' command</li>
 * <li>Solaris: uses '/usr/xpg4/bin/df -kP' command</li>
 * </ul>
 *
 * <p><b>Note:</b> This implementation relies on command-line utilities and may not work
 * on all platforms or configurations. For modern applications, consider using
 * {@link java.nio.file.FileStore} instead.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get free space in kilobytes for a specific path
 * long freeSpaceKb = FileSystemUtil.freeSpaceKb("/home");
 * System.out.println("Free space: " + freeSpaceKb + " KB");
 *
 * // Get free space for current working directory
 * long currentDirSpace = FileSystemUtil.freeSpaceKb();
 *
 * // With timeout
 * long freeSpace = FileSystemUtil.freeSpaceKb("C:\\", 5000);
 * }</pre>
 *
 * @version $Id: FileSystemUtils.java 1642799 2014-12-02 02:55:39Z sebb $
 */
@SuppressWarnings("java:S6548")
final class FileSystemUtil {

    /** Singleton instance, used mainly for testing. */
    private static final FileSystemUtil INSTANCE = new FileSystemUtil();

    /** Operating system state flag for error. */
    private static final int INIT_PROBLEM = -1;
    /** Operating system state flag for neither Unix nor Windows. */
    private static final int OTHER = 0;
    /** Operating system state flag for Windows. */
    private static final int WINDOWS = 1;
    /** Operating system state flag for Unix. */
    private static final int UNIX = 2;
    /** Operating system state flag for Posix flavour Unix. */
    private static final int POSIX_UNIX = 3;

    private static final int OS;

    /**  The path to df. */
    private static final String DF;

    static {
        String dfPath = "df";
        int os = OTHER;

        try {
            String osName = System.getProperty("os.name");
            if (osName == null) {
                throw new IOException("os.name not found");
            }
            osName = osName.toLowerCase(Locale.ENGLISH);
            // match
            if (osName.contains("windows")) {
                os = WINDOWS;
            } else if (osName.contains("linux") || osName.contains("mpe/ix") || osName.contains("freebsd") || osName.contains("irix")
                    || osName.contains("digital unix") || osName.contains("unix") || osName.contains("mac os x")) {
                os = UNIX;
            } else if (osName.contains("sun os") || osName.contains("sunos") || osName.contains("solaris")) {
                os = POSIX_UNIX;
                dfPath = "/usr/xpg4/bin/df"; //NOSONAR
            } else if (osName.contains("hp-ux") || osName.contains("aix")) {
                os = POSIX_UNIX;
            }

        } catch (final Exception ex) {
            os = INIT_PROBLEM;
        }
        OS = os;
        DF = dfPath;
    }

    private FileSystemUtil() {
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the free space on a drive or volume in kilobytes.
     *
     * <p>This method invokes the appropriate command line utility based on the
     * operating system to determine free space. The path must be a valid
     * file system path on the current platform.</p>
     *
     * <p>Platform-specific behavior:</p>
     * <ul>
     * <li>Windows: uses 'dir /a /-c' on the specified path</li>
     * <li>Unix/Linux/macOS: uses 'df -k' on the specified path</li>
     * <li>AIX/HP-UX: uses 'df -kP' on the specified path</li>
     * <li>Solaris: uses '/usr/xpg4/bin/df -kP' on the specified path</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Windows
     * long freeKb = FileSystemUtil.freeSpaceKb("C:");
     *
     * // Unix/Linux
     * long freeKb = FileSystemUtil.freeSpaceKb("/home");
     *
     * // Check if enough space (e.g., 100MB)
     * if (FileSystemUtil.freeSpaceKb("/tmp") < 100 * 1024) {
     *     System.err.println("Not enough space!");
     * }
     * }</pre>
     *
     * @param path the path to get free space for, not {@code null}, not empty on Unix
     * @return the amount of free space in kilobytes
     * @throws IllegalArgumentException if {@code path} is {@code null}, is empty on a Unix-like system, or contains
     *         a null byte or double-quote character on Windows
     * @throws IllegalStateException if an error occurred in initialization or the OS is not supported
     * @throws IOException if a Windows path cannot be normalized, the disk-space command cannot start,
     *         its output cannot be read or parsed as a non-negative long,
     *         it exits unsuccessfully, or the wait is interrupted
     */
    public static long freeSpaceKb(final String path) throws IllegalArgumentException, IllegalStateException, IOException {
        return freeSpaceKb(path, -1);
    }

    /**
     * Returns the free space on a drive or volume in kilobytes with a timeout.
     *
     * <p>This method is similar to {@link #freeSpaceKb(String)} but allows
     * specifying a timeout for the command execution. This is useful to prevent
     * hanging on unresponsive file systems.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     // Get free space with 5 second timeout
     *     long freeKb = FileSystemUtil.freeSpaceKb("C:", 5000);
     *     System.out.println("Free space: " + freeKb + " KB");
     * } catch (IOException e) {
     *     // Handle timeout or other errors
     *     System.err.println("Failed to get free space: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param path the path to get free space for, not {@code null}, not empty on Unix
     * @param timeout the timeout in milliseconds, or 0 or negative for no timeout
     * @return the amount of free space in kilobytes
     * @throws IllegalArgumentException if {@code path} is {@code null}, is empty on a Unix-like system, or contains
     *         a null byte or double-quote character on Windows
     * @throws IllegalStateException if an error occurred in initialization or the OS is not supported
     * @throws IOException if a Windows path cannot be normalized, the disk-space command cannot start,
     *         its output cannot be read or parsed as a non-negative long,
     *         it exits unsuccessfully, the wait is interrupted, or a positive timeout expires
     */
    public static long freeSpaceKb(final String path, final long timeout) throws IllegalArgumentException, IllegalStateException, IOException {
        return INSTANCE.freeSpaceOS(path, OS, true, timeout);
    }

    /**
     * Returns the free space in the current working directory in kilobytes.
     *
     * <p>This is equivalent to calling:</p>
     * <pre>{@code
     * return freeSpaceKb(new File(".").getAbsolutePath());
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long freeKb = FileSystemUtil.freeSpaceKb();
     * System.out.println("Current directory free space: " + freeKb + " KB");
     * }</pre>
     *
     * @return the amount of free space in the current directory in kilobytes
     * @throws IllegalStateException if an error occurred in initialization or the OS is not supported
     * @throws IOException if the disk-space command cannot start, its output cannot be read or parsed as a non-negative long,
     *         it exits unsuccessfully, or the wait is interrupted
     */
    public static long freeSpaceKb() throws IllegalStateException, IOException {
        return freeSpaceKb(-1);
    }

    /**
     * Returns the free space in the current working directory in kilobytes with a timeout.
     *
     * <p>This is equivalent to calling:</p>
     * <pre>{@code
     * return freeSpaceKb(new File(".").getAbsolutePath(), timeout);
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get free space in current directory with 3 second timeout
     * long freeKb = FileSystemUtil.freeSpaceKb(3000);
     * }</pre>
     *
     * @param timeout the timeout in milliseconds, or 0 or negative for no timeout
     * @return the amount of free space in the current directory in kilobytes
     * @throws IllegalStateException if an error occurred in initialization or the OS is not supported
     * @throws IOException if the disk-space command cannot start, its output cannot be read or parsed as a non-negative long,
     *         it exits unsuccessfully, the wait is interrupted, or a positive timeout expires
     */
    public static long freeSpaceKb(final long timeout) throws IllegalStateException, IOException {
        return freeSpaceKb(new File(".").getAbsolutePath(), timeout);
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the free space on a drive or volume in a cross-platform manner.
     * Note that some OS's are NOT currently supported, including OS/390.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileSystemUtil.freeSpaceKb("C:");        // uses a Windows path
     * FileSystemUtil.freeSpaceKb("/volume");   // uses a *nix path
     * }</pre>
     *
     * <p>The free space is calculated via the command line.
     * It uses 'dir /a /-c' on Windows and <i>df</i> on *nix.</p>
     *
     * @param path the path to get free space for, not {@code null}, not empty on Unix
     * @param os the operating system code
     * @param kb whether to normalize to kilobytes
     * @param timeout the timeout amount in milliseconds or no timeout if the value
     *  is zero or less; a positive timeout covers both process exit and reading its output
     * @return the amount of free space in kilobytes if {@code kb} is {@code true}; otherwise in bytes on Windows, or in the df command's default block units on Unix
     * @throws IllegalArgumentException if {@code path} is {@code null}, is empty on a Unix-like system, or contains
     *         a null byte or double-quote character on Windows
     * @throws IllegalStateException if the operating system is unsupported, or if an error
     *         occurred during initialization of the OS detection
     * @throws IOException if a Windows path cannot be normalized, the disk-space command cannot start,
     *         its output cannot be read or parsed as a non-negative long,
     *         it exits unsuccessfully, the wait is interrupted, or a positive timeout expires
     */
    long freeSpaceOS(final String path, final int os, final boolean kb, final long timeout)
            throws IllegalArgumentException, IllegalStateException, IOException {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        switch (os) {
            case WINDOWS:
                return kb ? freeSpaceWindows(path, timeout) / 1024 : freeSpaceWindows(path, timeout);
            case UNIX:
                return freeSpaceUnix(path, kb, false, timeout);
            case POSIX_UNIX:
                return freeSpaceUnix(path, kb, true, timeout);
            case OTHER:
                throw new IllegalStateException("Unsupported operating system");
            default:
                throw new IllegalStateException("Exception caught when determining operating system");
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Finds free space on the Windows platform using the <i>dir</i> command.
     *
     * @param path the path to get free space for, including the colon
     * @param timeout the timeout amount in milliseconds or no timeout if the value
     *  is zero or less
     * @return the amount of free disk space in bytes
     * @throws IllegalArgumentException if {@code path} contains a null byte or double-quote character
     * @throws IOException if {@code path} cannot be normalized, the command cannot start, its output cannot be read
     *         or parsed as a non-negative long, it exits unsuccessfully, the wait is interrupted, or a positive timeout expires
     */
    long freeSpaceWindows(String path, final long timeout) throws IllegalArgumentException, IOException {
        path = FilenameUtil.normalize(path, false);

        if (path == null) {
            throw new IOException("Invalid path: unable to normalize");
        }

        if (path.indexOf('"') >= 0) {
            throw new IllegalArgumentException("Path must not contain quote characters: " + path);
        }

        // Expand one child-process variable inside quotes. Its value is not recursively expanded by cmd.
        final String[] cmdAttrs = { "cmd.exe", "/d", "/v:off", "/C", path.isEmpty() ? "dir /a /-c" : "dir /a /-c \"%ABACUS_FREE_SPACE_PATH%\"" };

        // read in the output of the command to an ArrayList
        final List<String> lines = performCommand(cmdAttrs, Integer.MAX_VALUE, timeout, java.util.Map.of("ABACUS_FREE_SPACE_PATH", path));

        // now iterate over the lines we just read and find the LAST
        // non-empty line (the free space bytes should be in the last element
        // of the ArrayList anyway, but this will ensure it works even if it's
        // not, still assuming it is on the last non-blank line)
        for (int i = lines.size() - 1; i >= 0; i--) {
            final String line = lines.get(i);
            if (!line.isEmpty()) {
                return parseDir(line, path);
            }
        }
        // all lines are blank
        throw new IOException("Command line 'dir /-c' did not return any info " + "for path '" + path + "'"); //NOSONAR
    }

    /**
     * Parses the Windows dir response last line.
     *
     * @param line the line to parse
     * @param path the path that was sent
     * @return the amount of free disk space in bytes extracted from the dir command output
     * @throws NullPointerException if {@code line} is {@code null}
     * @throws IOException if the output contains no digits, or the extracted byte count is not a non-negative long
     */
    long parseDir(final String line, final String path) throws NullPointerException, IOException {
        // read from the end of the line to find the last numeric
        // character on the line, then continue until we find the first
        // non-numeric character, and everything between that and the last
        // numeric character inclusive is our free space bytes count
        int bytesStart = 0;
        int bytesEnd = 0;
        int j = line.length() - 1;
        innerLoop1: while (j >= 0) { //NOSONAR
            final char c = line.charAt(j);
            if (Character.isDigit(c)) {
                // found the last numeric character, this is the end of
                // the free space bytes count
                bytesEnd = j + 1;
                //noinspection UnnecessaryLabelOnBreakStatement
                break innerLoop1;
            }
            j--;
        }
        innerLoop2: while (j >= 0) { //NOSONAR
            final char c = line.charAt(j);
            if (!Character.isDigit(c) && c != ',' && c != '.') {
                // found the next non-numeric character, this is the
                // beginning of the free space bytes count
                bytesStart = j + 1;
                //noinspection UnnecessaryLabelOnBreakStatement
                break innerLoop2;
            }
            j--;
        }
        // bytesEnd stays 0 only when innerLoop1 found no digit at all; j < 0 alone is not a valid
        // discriminator because innerLoop2 also drives j below 0 when the digits reach index 0
        // (a bare numeric line), which is a valid parse with bytesStart == 0.
        if (bytesEnd == 0) {
            throw new IOException("Command line 'dir /-c' did not return valid info " + "for path '" + path + "'");
        }

        // remove commas and dots in the bytes count
        final StringBuilder buf = new StringBuilder(line.substring(bytesStart, bytesEnd));
        for (int k = 0; k < buf.length(); k++) {
            if (buf.charAt(k) == ',' || buf.charAt(k) == '.') {
                buf.deleteCharAt(k--);
            }
        }
        return parseBytes(buf.toString(), path);
    }

    //-----------------------------------------------------------------------

    /**
     * Finds free space on the *nix platform using the <i>df</i> command.
     *
     * @param path the path to get free space for
     * @param kb whether to normalize to kilobytes
     * @param posix whether to use the POSIX standard format flag
     * @param timeout the timeout amount in milliseconds or no timeout if the value
     *  is zero or less
     * @return the amount of free space in kilobytes if {@code kb} is {@code true}, otherwise in the df command's default block units
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IllegalArgumentException if the path is empty.
     * @throws IOException if the command cannot start, its output cannot be read or does not contain a non-negative long
     *         free-space count, it exits unsuccessfully, the wait is interrupted, or a positive timeout expires
     */
    long freeSpaceUnix(final String path, final boolean kb, final boolean posix, final long timeout)
            throws NullPointerException, IllegalArgumentException, IOException {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Path must not be empty");
        }

        // build and run the 'dir' command
        String flags = "-";
        if (kb) {
            flags += "k";
        }
        if (posix) {
            flags += "P";
        }
        // An absolute operand cannot be mistaken for a df option, even if the supplied name starts with '-'.
        final String operand = new File(path).getAbsolutePath();
        final String[] cmdAttrs = flags.length() > 1 ? new String[] { DF, flags, operand } : new String[] { DF, operand };

        // perform the command, asking for up to 3 lines (header, interesting, overflow)
        final List<String> lines = performCommand(cmdAttrs, 3, timeout);
        if (lines.size() < 2) {
            // unknown problem, throw exception
            throw new IOException("Command line '" + DF + "' did not return info as expected " + "for path '" + path + "'- response was " + lines); //NOSONAR
        }
        final String line2 = lines.get(1); // the line we're interested in

        // Now, we tokenize the string. The fourth element is what we want.
        StringTokenizer tok = new StringTokenizer(line2, " ");
        if (tok.countTokens() < 4) {
            // could be long Filesystem, thus data on third line
            if (tok.countTokens() == 1 && lines.size() >= 3) {
                final String line3 = lines.get(2); // the line may be interested in
                tok = new StringTokenizer(line3, " ");
            } else {
                throw new IOException("Command line '" + DF + "' did not return data as expected " + "for path '" + path + "'- check path is valid"); //NOSONAR
            }
        } else {
            tok.nextToken(); // Ignore Filesystem
        }

        if (tok.countTokens() < 3) {
            throw new IOException("Command line '" + DF + "' did not return data as expected for path '" + path + "' - check path is valid"); //NOSONAR
        }

        tok.nextToken(); // Ignore 1K-blocks
        tok.nextToken(); // Ignore Used
        final String freeSpace = tok.nextToken();
        return parseBytes(freeSpace, path);
    }

    //-----------------------------------------------------------------------

    /**
     * Parses a string representing a number of bytes and returns its {@code long} value.
     *
     * @param freeSpace the string to parse, expected to contain a non-negative integer
     * @param path the file system path associated with the query, used in error messages
     * @return the parsed number of bytes as a non-negative {@code long}
     * @throws IOException if {@code freeSpace} is not a valid non-negative integer,
     *         or if the parsed value is negative
     */
    long parseBytes(final String freeSpace, final String path) throws IOException {
        try {
            final long bytes = Long.parseLong(freeSpace);
            if (bytes < 0) {
                throw new IOException("Command line '" + DF + "' did not find free space in response " + "for path '" + path + "'- check path is valid");
            }
            return bytes;

        } catch (final NumberFormatException ex) {
            throw new IOException("Command line '" + DF + "' did not return numeric data as expected " + "for path '" + path + "'- check path is valid", ex);
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Performs the os command.
     *
     * @param cmdAttrs the command line parameters
     * @param max the maximum limit for the lines returned
     * @param timeout the timeout amount in milliseconds or no timeout if the value
     *  is zero or less
     * @return a list of output lines from the executed command
     * @throws IOException if the command cannot start, its output cannot be read or is empty, it exits unsuccessfully,
     *         the wait is interrupted, or a positive {@code timeout} expires before the process and output readers finish
     */
    List<String> performCommand(final String[] cmdAttrs, final int max, final long timeout) throws IOException {
        return performCommand(cmdAttrs, max, timeout, null);
    }

    private List<String> performCommand(final String[] cmdAttrs, final int max, final long timeout, final java.util.Map<String, String> environment)
            throws IOException {

        final long startNanos = System.nanoTime();
        final long timeoutNanos = timeout > 0 ? TimeUnit.MILLISECONDS.toNanos(timeout) : 0;
        final List<String> lines = new ArrayList<>(20);
        Process proc = null;
        InputStream in = null;
        OutputStream out = null;
        InputStream err = null;
        Thread outputGobbler = null;
        Thread errorGobbler = null;
        try { //NOSONAR
            if (environment == null) {
                proc = openProcess(cmdAttrs);
            } else {
                final ProcessBuilder builder = new ProcessBuilder(cmdAttrs);
                builder.environment().putAll(environment);
                proc = builder.start();
            }
            in = proc.getInputStream();
            out = proc.getOutputStream();
            err = proc.getErrorStream();

            // Both pipes must be drained while the process runs; otherwise a full native pipe can
            // block the process before waitFor observes its completion.
            final InputStream outputStream = in;
            final IOException[] outputFailure = new IOException[1];
            outputGobbler = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(IOUtil.newInputStreamReader(outputStream, Charset.defaultCharset()))) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        if (lines.size() < max) {
                            lines.add(line.toLowerCase(Locale.ENGLISH).trim());
                        }
                    }
                } catch (final IOException e) {
                    outputFailure[0] = e;
                }
            }, FileSystemUtil.class.getSimpleName() + "-stdout");
            outputGobbler.setDaemon(true);
            outputGobbler.start();

            final InputStream errStream = err;
            errorGobbler = new Thread(() -> {
                try {
                    final byte[] buf = new byte[1024];
                    while (errStream.read(buf) != -1) { //NOSONAR
                        // discard
                    }
                } catch (final IOException ignored) {
                    // ignore
                }
            }, FileSystemUtil.class.getSimpleName() + "-stderr");
            errorGobbler.setDaemon(true);
            errorGobbler.start();

            final boolean finished;
            if (timeout > 0) {
                final long remainingNanos = timeoutNanos - (System.nanoTime() - startNanos);
                finished = remainingNanos > 0 && proc.waitFor(remainingNanos, TimeUnit.NANOSECONDS);
            } else {
                proc.waitFor();
                finished = true;
            }

            if (!finished) {
                throw new IOException("Command line timed out after " + timeout + " ms for command " + Arrays.asList(cmdAttrs));
            }

            // A descendant may inherit either pipe and keep it open after the direct process exits.
            // Both readers share the process deadline instead of receiving fresh timeout budgets.
            joinGobbler(outputGobbler, startNanos, timeoutNanos, timeout, cmdAttrs);
            joinGobbler(errorGobbler, startNanos, timeoutNanos, timeout, cmdAttrs);

            if (outputFailure[0] != null) {
                throw outputFailure[0];
            }

            if (proc.exitValue() != 0) {
                // os command problem, throw exception
                throw new IOException("Command line returned OS error code '" + proc.exitValue() + "' for command " + Arrays.asList(cmdAttrs));
            }
            if (lines.isEmpty()) {
                // unknown problem, throw exception
                throw new IOException("Command line did not return any info " + "for command " + Arrays.asList(cmdAttrs));
            }
            return lines;

        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Command line threw an InterruptedException " + "for command " + Arrays.asList(cmdAttrs) + " timeout=" + timeout, ex);
        } finally {
            final boolean outputReaderAlive = outputGobbler != null && outputGobbler.isAlive();
            final boolean errorReaderAlive = errorGobbler != null && errorGobbler.isAlive();
            if (outputReaderAlive) {
                outputGobbler.interrupt();
            }
            if (errorReaderAlive) {
                errorGobbler.interrupt();
            }

            final Process processToClose = proc;
            final InputStream outputToClose = in;
            final OutputStream inputToClose = out;
            final InputStream errorToClose = err;
            final Runnable cleanup = () -> {
                if (processToClose != null) {
                    if (processToClose.isAlive()) {
                        processToClose.destroyForcibly();
                    } else {
                        processToClose.destroy();
                    }
                }
                IOUtil.closeQuietly(outputToClose);
                IOUtil.closeQuietly(inputToClose);
                IOUtil.closeQuietly(errorToClose);
            };

            if (outputReaderAlive || errorReaderAlive) {
                // Closing a native pipe (including inside Process.destroy) can block behind a read
                // held open by an inherited child handle. Cleanup must not undo timeout/interruption.
                final Thread cleanupThread = new Thread(cleanup, FileSystemUtil.class.getSimpleName() + "-cleanup");
                cleanupThread.setDaemon(true);
                cleanupThread.start();
            } else {
                cleanup.run();
            }
        }
    }

    private void joinGobbler(final Thread gobbler, final long startNanos, final long timeoutNanos, final long timeout, final String[] cmdAttrs)
            throws InterruptedException, IOException {
        if (timeout <= 0) {
            gobbler.join();
            return;
        }

        while (gobbler.isAlive()) {
            final long remainingNanos = timeoutNanos - (System.nanoTime() - startNanos);
            if (remainingNanos <= 0) {
                throw new IOException("Command line timed out after " + timeout + " ms for command " + Arrays.asList(cmdAttrs));
            }
            TimeUnit.NANOSECONDS.timedJoin(gobbler, remainingNanos);
        }
    }

    /**
     * Opens the process to the operating system.
     *
     * @param cmdAttrs the command line parameters
     * @return the newly created Process
     * @throws IOException if the executable cannot be found or the operating-system process cannot be started
     */
    Process openProcess(final String[] cmdAttrs) throws IOException {
        return Runtime.getRuntime().exec(cmdAttrs);
    }

}
