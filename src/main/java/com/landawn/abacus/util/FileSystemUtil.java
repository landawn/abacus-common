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

/**
 * <p>This class is copied from Apache Commons IO developed at The Apache Software Foundation
 * under the Apache License 2.0.</p>
 * 
 * General File System utilities for checking disk space.
 * 
 * <p>This class provides static utility methods for querying file system
 * free space by invoking native operating system commands. It supports
 * Windows, Unix, and POSIX-compliant systems.</p>
 * 
 * <p>The class works by executing platform-specific commands:</p>
 * <ul>
 * <li>Windows: uses 'dir /-c' command</li>
 * <li>Unix/Linux: uses 'df -k' command</li>
 * <li>AIX/HP-UX: uses 'df -kP' command</li>
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
@SuppressWarnings({ "java:S6548" })
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

    /**
     * Instances should NOT be constructed in standard programming.
     */
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
     * <li>Windows: uses 'dir /-c' on the specified path</li>
     * <li>Unix/Linux: uses 'df -k' on the specified path</li>
     * <li>AIX/HP-UX: uses 'df -kP' on the specified path</li>
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
     * @param path the path to get free space for, not null, not empty on Unix
     * @return the amount of free space in kilobytes
     * @throws IOException if an error occurs when finding the free space
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialization or OS not supported
     */
    public static long freeSpaceKb(final String path) throws IOException {
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
     * @param path the path to get free space for, not null, not empty on Unix
     * @param timeout the timeout in milliseconds, or 0 or negative for no timeout
     * @return the amount of free space in kilobytes
     * @throws IOException if an error occurs when finding the free space
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialization or OS not supported
     */
    public static long freeSpaceKb(final String path, final long timeout) throws IOException {
        return INSTANCE.freeSpaceOS(path, OS, true, timeout);
    }

    /**
     * Returns the free space in the current working directory in kilobytes.
     * 
     * <p>This is equivalent to calling:</p>
     * <pre>{@code
     * freeSpaceKb(new File(".").getAbsolutePath())
     * }</pre>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long freeKb = FileSystemUtil.freeSpaceKb();
     * System.out.println("Current directory free space: " + freeKb + " KB");
     * }</pre>
     *
     * @return the amount of free space in the current directory in kilobytes
     * @throws IOException if an error occurs when finding the free space
     * @throws IllegalStateException if an error occurred in initialization
     */
    public static long freeSpaceKb() throws IOException {
        return freeSpaceKb(-1);
    }

    /**
     * Returns the free space in the current working directory in kilobytes with a timeout.
     * 
     * <p>This is equivalent to calling:</p>
     * <pre>{@code
     * freeSpaceKb(new File(".").getAbsolutePath(), timeout)
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
     * @throws IOException if an error occurs when finding the free space
     * @throws IllegalStateException if an error occurred in initialization
     */
    public static long freeSpaceKb(final long timeout) throws IOException {
        return freeSpaceKb(new File(".").getAbsolutePath(), timeout);
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the free space on a drive or volume in a cross-platform manner.
     * Note that some OS's are NOT currently supported, including OS/390.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FileSystemUtils.freeSpace("C:");  // Windows
     * FileSystemUtils.freeSpace("/volume");  // *nix
     * }</pre>
     * The free space is calculated via the command line.
     * It uses 'dir /-c' on Windows and <i>df</i> on *nix.
     *
     * @param path the path to get free space for, not {@code null}, not empty on Unix
     * @param os the operating system code
     * @param kb whether to normalize to kilobytes
     * @param timeout the timeout amount in milliseconds or no timeout if the value
     *  is zero or less
     * @return the amount of free disk space in bytes or kilobytes depending on the kb parameter
     * @throws IOException if an error occurs when finding the free space
     * @throws IllegalArgumentException if the path is invalid
     * @throws IllegalStateException if an error occurred in initialisation
     */
    long freeSpaceOS(final String path, final int os, final boolean kb, final long timeout) throws IOException {
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
     * Find free space on the Windows platform using the <i>dir</i> command.
     *
     * @param path the path to get free space for, including the colon
     * @param timeout the timeout amount in milliseconds or no timeout if the value
     *  is zero or less
     * @return the amount of free disk space in bytes
     * @throws IOException if an error occurs
     */
    long freeSpaceWindows(String path, final long timeout) throws IOException {
        path = FilenameUtil.normalize(path, false);
        if (!path.isEmpty() && path.charAt(0) != '"') {
            path = "\"" + path + "\"";
        }

        // build and run the 'dir' command
        final String[] cmdAttrs = { "cmd.exe", "/C", "dir /a /-c " + path };

        // read in the output of the command to an ArrayList
        final List<String> lines = performCommand(cmdAttrs, Integer.MAX_VALUE, timeout);

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
     * @throws IOException if an error occurs
     */
    long parseDir(final String line, final String path) throws IOException {
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
        if (j < 0) {
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
     * Find free space on the *nix platform using the <i>df</i> command.
     *
     * @param path the path to get free space for
     * @param kb whether to normalize to kilobytes
     * @param posix whether to use the POSIX standard format flag
     * @param timeout the timeout amount in milliseconds or no timeout if the value
     *  is zero or less
     * @return the amount of free disk space in kilobytes
     * @throws IOException if an error occurs
     */
    long freeSpaceUnix(final String path, final boolean kb, final boolean posix, final long timeout) throws IOException {
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
        final String[] cmdAttrs = flags.length() > 1 ? new String[] { DF, flags, path } : new String[] { DF, path };

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
        tok.nextToken(); // Ignore 1K-blocks
        tok.nextToken(); // Ignore Used
        final String freeSpace = tok.nextToken();
        return parseBytes(freeSpace, path);
    }

    //-----------------------------------------------------------------------

    /**
     * Parses the bytes from a string.
     *
     * @param freeSpace the free space string
     * @param path the path being queried
     * @return the number of bytes parsed from the string
     * @throws IOException if an error occurs
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
     * @throws IOException if an error occurs
     */
    List<String> performCommand(final String[] cmdAttrs, final int max, final long timeout) throws IOException {

        final List<String> lines = new ArrayList<>(20);
        Process proc = null;
        InputStream in = null;
        OutputStream out = null;
        InputStream err = null;
        BufferedReader inr = null;
        try { //NOSONAR

            final Thread monitor = ThreadMonitor.start(timeout);

            proc = openProcess(cmdAttrs);
            in = proc.getInputStream();
            out = proc.getOutputStream();
            err = proc.getErrorStream();
            // default charset is most likely appropriate here
            inr = new BufferedReader(IOUtil.newInputStreamReader(in, Charset.defaultCharset()));
            String line = inr.readLine();
            while (line != null && lines.size() < max) {
                line = line.toLowerCase(Locale.ENGLISH).trim();
                lines.add(line);
                line = inr.readLine();
            }

            proc.waitFor();

            ThreadMonitor.stop(monitor);

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
            throw new IOException("Command line threw an InterruptedException " + "for command " + Arrays.asList(cmdAttrs) + " timeout=" + timeout, ex);
        } finally {
            IOUtil.closeQuietly(in);
            IOUtil.closeQuietly(out);
            IOUtil.closeQuietly(err);
            IOUtil.closeQuietly(inr);
            if (proc != null) {
                proc.destroy();
            }
        }
    }

    /**
     * Opens the process to the operating system.
     *
     * @param cmdAttrs the command line parameters
     * @return the newly created Process
     * @throws IOException if an error occurs
     */
    Process openProcess(final String[] cmdAttrs) throws IOException {
        return Runtime.getRuntime().exec(cmdAttrs);
    }

}
