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
import java.io.EOFException;
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
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import com.landawn.abacus.util.stream.Stream;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

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
 *   <li><b>Stream Processing:</b> Stream utilities with explicit resource-ownership contracts</li>
 *   <li><b>Compression Support:</b> Built-in support for ZIP, GZIP, Snappy, LZ4, and Brotli compression</li>
 *   <li><b>NIO Integration:</b> Modern NIO.2 operations with Path and Channel support</li>
 *   <li><b>Charset Handling:</b> Explicit charset conversion utilities for reads and writes</li>
 *   <li><b>Memory Mapping:</b> Support for memory-mapped files for large file operations</li>
 *   <li><b>Parallel Processing:</b> Multi-threaded file processing with configurable thread pools</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Resource Management:</b> Methods that open resources internally close them where documented;
 *       callers remain responsible for closing streams, readers, writers, and iterators returned to them</li>
 *   <li><b>Error Handling:</b> I/O failures are either declared or wrapped in {@link UncheckedIOException},
 *       as specified by each method</li>
 *   <li><b>Performance First:</b> Optimized algorithms with minimal object allocation</li>
 *   <li><b>Null Handling:</b> Accepted {@code null} values and validation behavior are method-specific</li>
 *   <li><b>Cross-Platform:</b> Platform-independent file operations and path handling</li>
 *   <li><b>Memory Efficient:</b> Streaming operations for large files to minimize memory usage</li>
 * </ul>
 *
 * <p><b>Contract notes:</b>
 * <ul>
 *   <li><b>Which exception for a bad path:</b> A {@code null} path argument is a programming error and is
 *       reported as {@link IllegalArgumentException}, never as a {@link NullPointerException} from the
 *       underlying {@code java.io} constructor. That holds for <i>every</i> path argument - a source or a
 *       destination, a {@link File}, a {@link Path} or a file-name {@code String} - across the whole class: the
 *       {@code read} family, {@code write}, {@code append}, {@code copyFile}, {@code copyToDirectory},
 *       {@code moveToDirectory}, {@code zip}/{@code unzip}, {@code split}/{@code merge}, {@code sizeOf} and the
 *       {@code newXxx} factories alike. For a <i>source</i> path, a path that does not exist or cannot be read is
 *       reported as {@link FileNotFoundException}. A path that exists but is the wrong kind - a file where a
 *       directory is required, or the reverse - is reported as {@link IllegalArgumentException}, and that half of
 *       the rule holds for a <i>destination</i> too: handing a directory to {@code write}, {@code append},
 *       {@code writeLine(s)}, {@code merge}, {@code zip}, {@code copyURLToFile} or a {@code newFileOutputStream}/
 *       {@code newFileWriter}/{@code newBufferedWriter} factory is a bad argument, not the platform's
 *       {@code "(Access is denied)"} / {@code "(Is a directory)"} open failure. A method and its
 *       overflow-free {@code *AsBigInteger} twin, or its {@code split}/{@code splitBySize} sibling, always report
 *       the same exception for the same input. One deliberate departure: a <i>destination</i> directory that
 *       exists but cannot be written to is an {@code IOException}, since that is an environment failure rather
 *       than a bad argument - and so is a destination whose parent directory cannot be created, including when
 *       an existing regular file sits where that directory would have to go ({@code "Failed to create parent
 *       directory"}): the argument itself is neither present nor of the wrong kind, and creating its parent is
 *       part of the operation. (The {@code sizeOf}/{@code sizeOfDirectory} family used to be a second departure,
 *       reading {@code null} as "absent"; it now follows the rule like everything else, except that an explicit
 *       {@code considerNonExistingFileAsEmpty} still answers {@code 0} without inspecting the argument.)
 *       <p>The same rule covers a caller-supplied <i>stream</i>: a {@code null} {@code InputStream},
 *       {@code Reader}, {@code OutputStream} or {@code Writer} is an {@code IllegalArgumentException} too,
 *       wherever it is the source or the target of an operation - including the shortcut paths, so
 *       {@code write(new byte[0], (OutputStream) null)} is rejected rather than quietly doing nothing. Three
 *       families answer {@code null} instead of rejecting it, and say so individually: the
 *       {@code contentEquals}/{@code contentEqualsIgnoreEOL} comparisons, the {@code isBufferedReader}/
 *       {@code isBufferedWriter} predicates, and the {@code close}/{@code delete} family.
 *       <p>Three families of <i>path</i> arguments answer as well, and together they are the complete list of
 *       exceptions to the rule above: the query predicates {@code isFile}, {@code isDirectory},
 *       {@code isRegularFile}, {@code isSymbolicLink} and {@code updateLastModified}, which report
 *       {@code false} for a {@code null} path; the name accessors {@code getFileExtension} and
 *       {@code getNameWithoutExtension}, which return {@code null}; and the listing family
 *       {@code listFiles}/{@code listDirectories}/{@code walk}, which return an empty result for a
 *       {@code null} or non-existent path, and for a directory the platform refuses to list
 *       ({@code File.listFiles()} answers {@code null} there, where {@code sizeOf} and the copy family
 *       report an error) - though a path that <i>exists</i> and is not a directory is a wrong-kind
 *       argument there like everywhere else. {@code renameTo} also answers {@code false} for a
 *       {@code null} source, after validating the new name.
 *       <p>The destination <i>buffer</i> of the low-level {@code read(source, buf, off, len)} overloads is the
 *       one caller-supplied argument that keeps {@code java.io}'s own answer: a {@code null} {@code byte[]} or
 *       {@code char[]} there is a {@link NullPointerException}, as it is for
 *       {@link InputStream#read(byte[], int, int)}, because a missing destination cannot mean anything. On the
 *       {@code write} side a {@code null} array means <i>empty</i> instead (see "Empty file writes"), or is
 *       reported as {@link IndexOutOfBoundsException} when an {@code offset}/{@code count} pair selects bytes
 *       from it.</li>
 *   <li><b>Checked vs unchecked:</b> Which of the two a method uses follows one rule: <b>a method that hands
 *       back content or an answer wraps its I/O failure in {@link UncheckedIOException}; a method that changes the
 *       filesystem declares a checked {@code IOException}.</b> So the whole {@code read}/{@code readAll} family,
 *       {@code forEachLine}, {@code sizeOf}, {@code contentEquals}, {@code map}, {@code freeDiskSpaceInKB} and the
 *       {@code newXxx} factories are unchecked, while {@code write}, {@code append}, {@code copyFile},
 *       {@code copyToDirectory}, {@code moveToDirectory}, {@code zip}, {@code unzip}, {@code split},
 *       {@code splitBySize}, {@code splitByLine} and {@code merge} are checked. Inverse operations therefore always agree:
 *       {@code zip}/{@code unzip} and {@code split}/{@code merge} are both checked, so a round trip needs one
 *       {@code catch}, not two shapes.
 *       <p>Two deliberate departures. The low-level {@code read(source, buf, off, len)} overloads stay checked:
 *       they mirror {@link InputStream#read(byte[], int, int)}, return a count rather than content, and are used
 *       inside loops in code that already handles {@code IOException}. And {@code createFileIfNotExists} and
 *       {@link #touch(File)} are unchecked despite mutating, because they are the convenience twins of the
 *       package-private checked {@code createNewFileIfNotExists}.
 *       <p><b>{@code UncheckedIOException} here always means
 *       {@link com.landawn.abacus.exception.UncheckedIOException}</b>, not {@link java.io.UncheckedIOException}.
 *       The two are unrelated types that share a simple name, so a {@code catch (UncheckedIOException e)}
 *       written against the {@code java.io} one still <i>compiles</i> - it is an unchecked type - and then
 *       silently never fires. Import the {@code com.landawn.abacus.exception} one, or write it out, in any file
 *       that also has the {@code java.io} name in scope. The usage examples below all mean this class's
 *       type.</li>
 *   <li><b>Resource ownership:</b> Streams, readers, and writers supplied by the caller are not closed.
 *       Resources opened internally are always closed, including on failure. Where an operation opens a
 *       single resource, a close failure is reported: added as a suppressed exception when a primary failure
 *       is already in flight, and thrown otherwise ({@code forEachLine} over a {@code File} or a
 *       {@code Collection} is the exception: it releases its file iterators through the logged-and-discarded rule
 *       below even when the source was a single file). Where several resources are released together at the end
 *       of a multi-file operation - the {@code forEachLine} overloads taking a {@code Collection<File>} - close
 *       failures are logged and discarded so that the remaining resources are still released.</li>
 *   <li><b>How far a caller-owned source is read:</b> An overload that decodes an {@code InputStream} into
 *       characters or lines wraps it in a decoder/reader, and buffering and charset decoding may consume more
 *       of the stream than the returned content accounts for. There is no way to decode a byte stream as text
 *       and stay exact, so an {@code InputStream} is never safe to continue from.
 *       <p>A {@code Reader} is. Every overload that takes one and returns a <i>bounded</i> result leaves it
 *       positioned immediately after what it handed back, so the caller can carry on reading: the character
 *       slicers {@code readChars}, {@code readToString}, {@code read(Reader, buf, off, len)}, {@code skip},
 *       {@code skipFully} and the {@code write}/{@code append} forms taking a {@code count}, and the line
 *       slicers {@code readFirstLine}, {@code readLine(Reader, lineIndex)} and
 *       {@code readLines(Reader, offset, count)}. The line slicers pay for it by reading one character at a
 *       time, and by needing one character of look-ahead to tell a lone {@code '\r'} from the first half of
 *       {@code "\r\n"}: a reader supporting {@link Reader#mark(int)} is put back exactly, and for one that
 *       does not, only a lone {@code '\r'} ending the last line of the call keeps a character the caller would
 *       otherwise still see. A source that is already a {@link java.io.BufferedReader} is read through
 *       directly and is exact either way.
 *       <p>The remaining {@code Reader} overloads read to end of input by definition, and say so individually:
 *       {@code readAllChars}, {@code readAllToString}, {@code readAllLines}, {@code readLastLine},
 *       {@code forEachLine}, and {@code write}/{@code append} without a {@code count}. {@code contentEquals}
 *       and {@code contentEqualsIgnoreEOL} stop at the first difference instead, leaving an unspecified
 *       position. None of those is meant to be continued from.</li>
 *   <li><b>{@code read(stream, buffer, off, len)}:</b> Unlike {@link InputStream#read(byte[], int, int)},
 *       this loops until {@code len} bytes/chars are filled, EOF occurs, or a read returns zero.</li>
 *   <li><b>Empty file writes:</b> Every {@code write}/{@code writeLines} overload targeting a {@code File}
 *       creates the file if missing and truncates it otherwise, <i>including</i> when the input is empty. Writing a
 *       {@code null} or empty array, a {@code null} or empty {@code CharSequence}, or a {@code null}/empty collection
 *       or iterator therefore leaves the target existing and empty rather than preserving its previous content, so a
 *       write is always a complete replacement. The {@code OutputStream}/{@code Writer} overloads write nothing for
 *       empty input but still flush when {@code flush} is {@code true}, and there a {@code null}
 *       {@code CharSequence} is written as the four-character text {@code "null"}, matching
 *       {@link Appendable#append(CharSequence)}. The {@code append} family is the mirror image: appending nothing
 *       creates a missing file but never truncates an existing one.
 *       <p>The rule above is about <i>emptiness</i>, and the {@code Object}-taking {@code writeLine} and
 *       {@code appendLine} are outside it: an {@code Object} is never empty, it is rendered with
 *       {@code N.toString(obj)}, and a {@code null} renders as the four-character text {@code "null"} there too.
 *       So {@code write((CharSequence) null, file)} leaves the file empty while {@code writeLine(null, file)}
 *       leaves it holding {@code "null\n"} - the two are different operations, not two spellings of one.</li>
 *   <li><b>Charset-aware text writes:</b> Every charset-aware {@code write} or {@code append} call that encodes
 *       characters into a file or {@code OutputStream} starts a separate encoding session. An encoder that
 *       emits a byte-order mark, such as UTF-16, emits one on each non-empty call, so two
 *       {@code write(chars, UTF_16, out)} calls produce two BOMs and the second one reads back as a
 *       {@code U+FEFF} character in the middle of the text rather than as a byte-order signal. Encode the
 *       whole text in one call, or write through a single long-lived {@code Writer}, when that matters.</li>
 *   <li><b>File-name decompression:</b> Every method that reads a {@code File} as <i>content</i> - the whole
 *       {@code read*} family ({@code readBytes}, {@code readAllBytes}, {@code readChars}, {@code readAllChars},
 *       {@code readToString}, {@code readAllToString}, {@code readLines}, {@code readAllLines},
 *       {@code readFirstLine}, {@code readLastLine}, {@code readLine} and {@code read}), plus
 *       {@code forEachLine(File, ...)} and {@code forEachLine(Collection<File>, ...)} - treats {@code .gz} as
 *       gzip and {@code .zip} as the first non-directory ZIP entry (case-insensitive). For a <i>file</i>
 *       {@code forEachLine} is simply the streaming form of {@code readAllLines} and decompresses the same way.
 *       The two part company on a <i>directory</i>, which is not a decoding question at all: {@code readAllLines}
 *       rejects one as a wrong-kind argument, while {@code forEachLine} accepts it and streams the lines of the
 *       regular files underneath it, recursively - links to files included, a linked directory not descended
 *       into, special files and dangling links left out, an unreadable file reported as an error.
 *       The methods that read a file as <i>bytes to be reproduced</i> read it literally: {@code split},
 *       {@code splitBySize}, {@code merge}, {@code zip}, {@code write(File, ..)}, {@code append(File, ..)},
 *       {@code copyFile}, {@code copyToDirectory}, {@code moveToDirectory}, {@code contentEquals(File, File)},
 *       {@code sizeOf} and the {@code newFileInputStream}/{@code newFileReader}/{@code newBufferedReader}/
 *       {@code newBufferedInputStream} factories. So {@code IOUtil.write(archive, out)} copies the compressed
 *       bytes while {@code IOUtil.readAllBytes(archive)} yields the decompressed ones, and {@code sizeOf} of a
 *       {@code .gz} is its size on disk, not the size of what it holds. Matching stream overloads read bytes
 *       literally, and file writes never compress based on the extension. {@code splitByLine} is the one
 *       <i>split</i> that decompresses, because it produces text parts rather than byte-preserving ones.</li>
 *   <li><b>{@code forEachLine} decoding:</b> The positional {@code forEachLine} overloads decode a {@code File},
 *       a {@code Collection<File>} or an {@code InputStream} as UTF-8. To read another encoding, pass a
 *       {@link LineIterationOptions} with a {@code charset}; the {@code Reader} overloads take characters that are
 *       already decoded and ignore that field. A byte sequence that is not valid for the chosen charset is
 *       replaced with {@code U+FFFD} rather than reported, so a mis-encoded file is processed silently.</li>
 *   <li><b>Byte-order marks:</b> A BOM is content, never a marker: nothing in this class strips one, and
 *       nothing writes one that the chosen {@link Charset} would not write anyway. Reading a UTF-8 file that
 *       starts with {@code EF BB BF} therefore yields a first character of {@code U+FEFF} - which is why a
 *       {@code readFirstLine} used as a CSV header does not match a plain {@code "id,name"}, and why a BOM'd
 *       and a non-BOM'd copy of the same text are not {@code contentEquals}. Strip it yourself, or read
 *       through a BOM-aware stream, when the source may carry one. UTF-16 and UTF-32 are the exception the
 *       JDK already handles: their decoders consume a leading BOM as the byte-order signal it is.</li>
 *   <li><b>Character-offset slicing:</b> The {@code offset}/{@code maxLen}/{@code count} pairs of
 *       {@code readChars}, {@code readToString}, {@code charsToBytes} and the {@code write}/{@code append}
 *       overloads that take a {@code char[]} with an {@code offset} and {@code count} all count {@code char}
 *       values, not code points, so a boundary can fall between the two halves of a surrogate pair. The
 *       resulting lone surrogate is kept in a {@code char[]} or {@code String} result, and is handed unchanged
 *       to a {@code Writer} destination, but is replaced by the charset's replacement byte (typically
 *       {@code '?'}) as soon as it is encoded - which a {@code File} or {@code OutputStream} destination does
 *       here, and a {@code FileWriter} destination does one layer down (a {@code StringWriter} keeps it).
 *       Slice on code-point boundaries if that matters.</li>
 *   <li><b>Parallel {@code forEachLine}:</b> Concurrency is requested through {@link LineIterationOptions};
 *       the overloads without it always read and process on the calling thread. When {@code readThreads} or
 *       {@code processThreads} is greater than zero, lines may be read and processed concurrently and unordered.
 *       A global {@code offset}/{@code count} across multiple files is then not a stable selection. The
 *       declared callback exception type propagates unchanged and unwrapped on both paths; an interrupt of the
 *       calling thread on the parallel path surfaces as {@code UncheckedInterruptedException} after a bounded
 *       cancellation wait, with the interrupt flag preserved. Use zero worker/reader threads for sequential
 *       ordered processing.</li>
 *   <li><b>Copy symlink policy:</b> {@code copyToDirectory} resolves the top-level source (a symlink to a
 *       file or a directory is followed so the referenced content is copied) but names the copy after the
 *       <i>link</i>, not its target. Nested symbolic links, including immediate children of
 *       {@code copyDirectory}, are copied as links and are not followed. {@code zip} follows the same
 *       top-level rule, naming the entries under the link's own name too, but cannot store a link: a nested
 *       link to a file is archived as that file, and a nested link to a directory, or a dangling one, is left
 *       out. {@code moveToDirectory} moves the link itself, under its own name. {@code copyFile} follows a
 *       link unless {@code NOFOLLOW_LINKS} is passed, in which case the link itself - dangling or not, to a
 *       file or to a directory - is what is copied. The listing, size and delete families, and the directory
 *       expansion of {@code forEachLine}, never descend into a
 *       linked directory, and the delete family unlinks a dangling link rather than treating it as
 *       absent. On Windows a directory junction ({@code mklink /J}, which {@link Files#isSymbolicLink(Path)}
 *       does not report as a link) counts as a linked directory for those families: it is never descended
 *       into or deleted through, and a dangling one is unlinked. The copy family follows a junction instead -
 *       Java cannot recreate one as a link - and copies its contents as a plain directory (a junction leading
 *       back to a directory on the path being copied, directly or through other junctions, or to the copy's
 *       destination, a directory above it or anything inside it, would copy for ever and is left out; any other
 *       junction is followed, so its target may be copied more than once), while {@code zip} leaves a nested
 *       junction out of the archive as it leaves out a nested directory link;
 *       a DANGLING nested junction is left out by the copy family and by {@code zip} alike (it can be neither
 *       followed nor recreated), as is a junction the platform cannot resolve (one beyond its reparse-point
 *       limit reads as dangling) and any other reparse point Java cannot follow, such as the symbolic link WSL
 *       writes on a Windows volume. A special file nested in a source tree - a FIFO, socket or device node - is
 *       left out by both as well (it cannot be recreated, and reading a FIFO blocks until a writer appears),
 *       while one named as the source itself is rejected as neither a file nor a directory. A copy never
 *       writes through a link already present at a nested destination path, live or dangling, to a file or to
 *       a directory: that entry counts as existing, and the copy is refused like any other overwrite (the
 *       caller's own {@code destDir} may be a link and is followed, as {@code cp -R} follows the directory it is
 *       given). {@code moveToDirectory} moves the
 *       junction itself. {@code isSymbolicLink(File)} keeps {@link Files#isSymbolicLink(Path)}'s answer and so
 *       still says {@code false} for a junction, and a dangling junction is likewise never treated as
 *       absent.</li>
 * </ul>
 *
 * <p><b>Parameter Conventions:</b>
 * <ul>
 *   <li><b>Offset Parameters:</b> Uses {@code offset/count/len} instead of {@code fromIndex/toIndex}</li>
 *   <li><b>Charset Handling:</b> Defaults to UTF-8 ({@code Charsets.UTF_8}) for consistent encoding (NOT the JVM platform default)</li>
 *   <li><b>Buffer Sizes:</b> Intelligent default buffer sizes with customization options</li>
 *   <li><b>Exception Handling:</b> {@code UncheckedIOException} wrapping for cleaner API usage</li>
 * </ul>
 *
 * <p><b>Core Operation Categories:</b>
 * <ul>
 *   <li><b>Stream Creation:</b> Factory methods for file, buffered, character, and compression streams</li>
 *   <li><b>File Operations:</b> Copy, move, delete, create operations with advanced options</li>
 *   <li><b>Directory Management:</b> Recursive operations, listing, traversal, and cleanup</li>
 *   <li><b>Compression/Decompression:</b> ZIP, GZIP, Snappy, Brotli format support</li>
 *   <li><b>Content Processing:</b> Line-by-line processing with parallel execution support</li>
 *   <li><b>File Splitting/Merging:</b> Large file handling with size-based, part-count and line-boundary splitting</li>
 *   <li><b>Content Comparison:</b> Byte-level and content-aware file comparison utilities</li>
 *   <li><b>URL/File Conversion:</b> Bidirectional conversion between URLs and File objects</li>
 * </ul>
 *
 * <p><b>Compression Formats Supported:</b>
 * <ul>
 *   <li><b>ZIP:</b> Standard ZIP compression with custom charset support</li>
 *   <li><b>GZIP:</b> GNU ZIP compression with configurable buffer sizes</li>
 *   <li><b>Snappy:</b> High-speed compression optimized for performance</li>
 *   <li><b>LZ4:</b> Block compression optimized for very high throughput (see {@code newLZ4Block*Stream})</li>
 *   <li><b>Brotli:</b> Modern compression algorithm with excellent compression ratios (decompression only)</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic file operations
 * IOUtil.copyFile(sourceFile, targetFile);
 * IOUtil.moveToDirectory(sourceFile, targetDirectory);
 * boolean success = IOUtil.deleteIfExists(file);
 *
 * // Caller-owned streams are closed with try-with-resources
 * try (InputStream is = IOUtil.newFileInputStream(file);
 *      OutputStream os = IOUtil.newFileOutputStream(targetFile)) {
 *     IOUtil.write(is, os);
 * }
 *
 * // Compression operations
 * IOUtil.zip(sourceFiles, targetZipFile);
 * IOUtil.unzip(zipFile, extractDirectory);
 *
 * // Directory operations
 * List<File> files = IOUtil.listFiles(directory, true, false);
 * IOUtil.deleteFilesFromDirectory(tempDirectory);
 *
 * // Large file processing
 * IOUtil.forEachLine(largeFile, line -> {
 *     // Process each line efficiently
 *     processLine(line);
 * });
 *
 * // File splitting for large files
 * IOUtil.splitBySize(largeFile, 1024 * 1024);   // Split into 1MB parts
 * IOUtil.split(file, 10);       // Split into 10 equal parts (exact byte offsets; a line may be cut in half)
 * IOUtil.splitByLine(file, 10); // Split into at most 10 parts, never cutting a line
 *
 * // Content comparison
 * boolean identical = IOUtil.contentEquals(file1, file2);
 * boolean sameIgnoreEOL = IOUtil.contentEqualsIgnoreEOL(file1, file2, "UTF-8");
 * }</pre>
 *
 * <p><b>Advanced Stream Operations:</b>
 * <pre>{@code
 * // Memory-mapped file operations for large files
 * try (FileInputStream fis = IOUtil.newFileInputStream(file);
 *      FileChannel channel = fis.getChannel()) {
 *     MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, file.length());
 *     // Process mapped buffer efficiently
 * }
 *
 * // Parallel line processing, and a non-UTF-8 source
 * // (LineIterationOptions is a nested type: import com.landawn.abacus.util.IOUtil.LineIterationOptions;)
 * IOUtil.forEachLine(file,
 *     IOUtil.LineIterationOptions.builder().readThreads(4).queueSize(1000).charset(StandardCharsets.ISO_8859_1).build(),
 *     line -> processLine(line));
 *
 * // Buffered stream creation with optimal sizes
 * try (BufferedReader reader = IOUtil.newBufferedReader(file, StandardCharsets.UTF_8);
 *      BufferedWriter writer = IOUtil.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
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
 *   <li><b>Line Processing:</b> O(n) with memory bounded primarily by the longest line and any configured work queue</li>
 *   <li><b>Compression:</b> Algorithm-dependent, optimized for speed vs. ratio trade-offs</li>
 *   <li><b>File Splitting:</b> O(n) with minimal memory overhead</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Shared State:</b> The utility has no caller-visible mutable state other than its cached host name</li>
 *   <li><b>Caller Resources:</b> Concurrent operations on the same file, stream, reader, writer, or callback require caller coordination</li>
 *   <li><b>Parallel Operations:</b> Selected line-processing overloads support configurable worker threads</li>
 * </ul>
 *
 * <p><b>Error Handling Strategy:</b>
 * <ul>
 *   <li><b>UncheckedIOException:</b> Wraps checked {@code IOException} for cleaner API usage</li>
 *   <li><b>Partial Results:</b> Mutating file operations are not transactional and may leave partial output after a failure</li>
 *   <li><b>Resource Cleanup:</b> Resources opened internally are closed; caller-supplied resources remain caller-owned unless documented otherwise</li>
 * </ul>
 *
 * <p><b>Platform Compatibility:</b>
 * <ul>
 *   <li><b>Cross-Platform Paths:</b> Handles platform-specific path separators automatically</li>
 *   <li><b>File System Features:</b> Adapts to file system capabilities (symlinks, permissions)</li>
 *   <li><b>Charset Handling:</b> Explicit charset conversion with UTF-8 as this class's default</li>
 *   <li><b>NIO.2 Integration:</b> Modern file-system operations through {@link Files} where appropriate</li>
 * </ul>
 *
 * <p><b>Integration with Java NIO:</b>
 * <ul>
 *   <li><b>Path Support:</b> Seamless integration with {@code java.nio.file.Path}</li>
 *   <li><b>Channel Operations:</b> Direct support for NIO channels for high-performance I/O</li>
 *   <li><b>File Attributes:</b> Advanced file attribute handling and manipulation</li>
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
 * <p><b>Usage Examples: File processing</b></p>
 * <pre>{@code
 * File logDirectory = new File("/var/logs");
 * File outputDirectory = new File("/processed");
 *
 * List<File> logFiles = IOUtil.listFiles(logDirectory, true, false)
 *     .stream()
 *     .filter(f -> f.getName().endsWith(".log"))
 *     .collect(Collectors.toList());
 *
 * for (File logFile : logFiles) {
 *     File outputFile = new File(outputDirectory, logFile.getName() + ".processed");
 *     List<String> processed = new ArrayList<>();
 *     IOUtil.forEachLine(logFile, line -> {
 *         String result = processLogLine(line);
 *         if (result != null) {
 *             processed.add(result);
 *         }
 *     });
 *     IOUtil.writeLines(processed, outputFile);
 * }
 *
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
// @ai-ignore review 2026-08-29: the int/long width of the slicing parameters is settled - by design / won't-fix.
// readBytes/readChars/readToString take (long offset, int maxLen) because the offset can exceed 2 GB while the
// result must fit an array; readLines takes (int offset, int count) because both bound an in-memory List; and
// forEachLine takes (long lineOffset, long count) because it streams and neither bound is materialized. Do not
// "unify" these to one width.
public final class IOUtil {

    private static final Logger logger = LoggerFactory.getLogger(IOUtil.class);

    // Q/A from AI:
    // Yes — for almost all new Java code, default to UTF-8 explicitly for file read/write.
    // Do not rely on the system default charset unless you are intentionally reading/writing files in the user’s local legacy encoding.
    // Since JDK 18, Java’s standard default charset is UTF-8 across platforms, except console I/O, via JEP 400.
    // But if your library or app supports Java 8/11/17, the platform default may still vary by OS/locale, especially on older Windows setups.
    static final Charset DEFAULT_CHARSET = Charsets.UTF_8; // library-wide default: ALWAYS UTF-8 (NOT the JVM platform default), for cross-platform consistency

    // ..
    private static final String JAVA_VENDOR_STR = "java.vendor";

    private static final String JAVA_VM_VENDOR_STR = "java.vm.vendor";

    private static final String ANDROID = "ANDROID";

    private static final String ZIP = ".zip";

    private static final String GZ = ".gz";

    // ..
    // No trimResults(): " .. " or "b " are legal, distinct path components; trimming them could
    // fabricate parent-directory traversal in simplifyPath (the Guava original does not trim).
    private static final Splitter pathSplitter = Splitter.with('/');

    /**
     * The file copy buffer size (8 MB).
     */
    private static final int FILE_COPY_BUFFER_SIZE = 8 * (1024 * 1024);

    // Note: earlier versions reflected into java.lang.StringCoding.encode/decode(Charset, char[], int, int) as a
    // zero-copy fast path. Those methods were removed in JDK 17, which is this library's minimum, so the lookup
    // could never succeed again: it only cost a Class.forName + setAccessible probe at class-initialization time
    // (and needed --add-opens on the module path). It has been deleted; charsToBytes/bytesToChars now always use
    // the plain String-based conversion.

    private static final String UNKNOWN_HOST_NAME = "UNKNOWN_HOST_NAME";

    private static final long HOST_NAME_RESOLVE_TIMEOUT_SECONDS = 5;

    /** Minimum delay before a failed or timed-out host-name resolution is attempted again. */
    private static final long HOST_NAME_RETRY_BACKOFF_MILLIS = 60 * 1000L;

    private static volatile String hostName;

    /** Guards the single in-flight host-name resolution shared by all callers. */
    private static final Object hostNameLock = new Object();

    private static ExecutorService hostNameResolver;

    private static Future<String> hostNameFuture;

    private static long hostNameRetryAtMillis;

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
     * The maximum memory available to the JVM in megabytes, sampled when this class is initialized.
     *
     * <p>{@link Runtime#maxMemory()} returns {@link Long#MAX_VALUE} when the heap is unbounded; that would
     * overflow to {@code -1} if narrowed directly, so the value is clamped to {@link Integer#MAX_VALUE}.
     * Treat {@code Integer.MAX_VALUE} as "no configured limit" rather than as a real measurement.
     */
    public static final int MAX_MEMORY_IN_MB = (int) Math.min(Integer.MAX_VALUE, Runtime.getRuntime().maxMemory() / (1024 * 1024));

    // ..
    /**
     * The operating system name.
     */
    public static final String OS_NAME = N.defaultIfNull(System.getProperty("os.name"), "");

    private static final String OS_NAME_UPPER_CASE = OS_NAME.toUpperCase(Locale.ROOT);

    /**
     * The operating system version. Never {@code null}: the empty string when the property is not set,
     * matching {@link #OS_NAME}.
     */
    public static final String OS_VERSION = N.defaultIfNull(System.getProperty("os.version"), "");

    /**
     * The operating system architecture. Never {@code null}: the empty string when the property is not set,
     * matching {@link #OS_NAME}.
     */
    public static final String OS_ARCH = N.defaultIfNull(System.getProperty("os.arch"), "");

    //..
    /**
     * Flag indicating whether the current operating system is Windows.
     */
    public static final boolean IS_OS_WINDOWS = OS_NAME_UPPER_CASE.contains("WINDOWS");

    /**
     * Flag indicating whether the current operating system is Mac.
     */
    public static final boolean IS_OS_MAC = OS_NAME_UPPER_CASE.contains("MAC");

    /**
     * Flag indicating whether the current operating system is Mac OS X.
     */
    public static final boolean IS_OS_MAC_OSX = OS_NAME_UPPER_CASE.contains("MAC OS X");

    /**
     * Flag indicating whether the current operating system is Linux.
     */
    public static final boolean IS_OS_LINUX = OS_NAME_UPPER_CASE.contains("LINUX");

    /**
     * Flag indicating whether the current platform is Android.
     */
    public static final boolean IS_PLATFORM_ANDROID = N.defaultIfNull(System.getProperty(JAVA_VENDOR_STR), "").toUpperCase(Locale.ROOT).contains(ANDROID)
            || N.defaultIfNull(System.getProperty(JAVA_VM_VENDOR_STR), "").toUpperCase(Locale.ROOT).contains(ANDROID);

    // ..
    /**
     * The Java home directory.
     */
    public static final String JAVA_HOME = System.getProperty("java.home");

    /**
     * The Java version as a {@link JavaVersion} object, resolved when this class is initialized.
     *
     * <p>Resolved from {@code java.version}, falling back to {@code java.specification.version} and then to
     * {@link JavaVersion#JAVA_RECENT}. It is never {@code null} and resolving it never throws: this constant
     * is a convenience that no I/O operation in this class reads, so a JVM whose version string cannot be
     * parsed must not be able to take the rest of the class down with it.
     *
     * @see JavaVersion#of(String)
     */
    public static final JavaVersion JAVA_VERSION = resolveJavaVersion();

    /**
     * Resolves {@link #JAVA_VERSION} without ever throwing.
     *
     * <p>This runs inside this class's static initializer, where an exception is not a failed call but an
     * {@link ExceptionInInitializerError} followed by a {@code NoClassDefFoundError} on every later touch of
     * {@code IOUtil} - the whole utility, and everything in this library that reaches it, permanently
     * unusable. {@code JavaVersion.of(System.getProperty("java.version"))} could do exactly that: a
     * {@code java.version} that is absent, or that carries a vendor spelling no parser can be expected to
     * know (Android has reported {@code "0"}), is rejected with {@code IllegalArgumentException}.
     *
     * <p>{@code java.specification.version} is the fallback because it is always a plain version number,
     * never an implementation string - it is also the property {@link JavaVersion}'s own examples read.
     * {@link JavaVersion#JAVA_RECENT} is the last resort; it resolves without consulting anything that can
     * fail, and treating an unidentifiable JVM as a recent one is the safer of the two guesses.
     *
     * @return the resolved version; never {@code null}.
     */
    private static JavaVersion resolveJavaVersion() {
        for (final String propertyName : new String[] { "java.version", "java.specification.version" }) {
            try {
                final String value = System.getProperty(propertyName);

                if (value != null && !value.isEmpty()) {
                    return JavaVersion.of(value);
                }
            } catch (final RuntimeException e) { // NOSONAR - deliberately broad: nothing may escape a static initializer
                // Unparseable or unreadable; try the next property.
            }
        }

        return JavaVersion.JAVA_RECENT;
    }

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

    // ..
    static final String JAVA_AWT_FONTS = System.getProperty("java.awt.fonts");

    static final String JAVA_AWT_GRAPHICSENV = System.getProperty("java.awt.graphicsenv");

    static final String JAVA_AWT_HEADLESS = System.getProperty("java.awt.headless");

    static final String JAVA_AWT_PRINTERJOB = System.getProperty("java.awt.printerjob");

    static final String JAVA_UTIL_PREFS_PREFERENCES_FACTORY = System.getProperty("java.util.prefs.PreferencesFactory");

    // ..
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
     * The user's country or region, or {@code null} if neither {@code user.country} nor the legacy
     * {@code user.region} spelling is set.
     */
    public static final String USER_COUNTRY = resolveUserCountry();

    /**
     * Resolves {@link #USER_COUNTRY} from {@code user.country}, falling back to {@code user.region}, which is
     * the spelling some JVMs use instead.
     *
     * @return the country/region code, or {@code null} if neither property is set.
     */
    @MayReturnNull
    private static String resolveUserCountry() {
        final String country = System.getProperty("user.country");

        return country == null ? System.getProperty("user.region") : country;
    }

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
     * The Unix line separator string ({@code "\n"}).
     */
    public static final String LINE_SEPARATOR_UNIX = "\n";

    /**
     * The Windows line separator string ({@code "\r\n"}).
     * @see System#lineSeparator()
     * @deprecated use {@link #LINE_SEPARATOR_UNIX} instead. It's recommended to use <i>\n</i> as the line separator on all platforms.
     * It will make things easier when files are shared between different OS platforms. Windows can handle <i>\n</i> correctly.
     */
    @Deprecated
    public static final String LINE_SEPARATOR_WINDOWS = "\r\n";

    /**
     * Current path retrieved by {@code new File("./").getAbsolutePath()}, with the trailing '.' removed.
     *
     * <p>The trailing directory separator is kept, so this is {@code "C:\work\"} / {@code "/home/me/"} where
     * {@link #USER_DIR} is {@code "C:\work"} / {@code "/home/me"}: the two name the same directory in two
     * spellings, and the value has been this way since the constant was introduced.
     */
    public static final String CURRENT_DIR;

    static {
        final String path = new File("./").getAbsolutePath();
        CURRENT_DIR = path.charAt(path.length() - 1) == '.' ? path.substring(0, path.length() - 1) : path;
    }

    // ..
    /**
     * Constant representing End-Of-File (or stream).
     */
    public static final int EOF = -1;

    /** Shared "no slicing, caller-thread reading and processing, UTF-8" options for the {@code forEachLine} builder overloads. */
    private static final LineIterationOptions DEFAULT_LINE_ITERATION_OPTIONS = LineIterationOptions.builder().build();

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> all_files_filter = (parentDir, file) -> true;

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> directories_excluded_filter = (parentDir, file) -> !file.isDirectory();

    /**
     * The entries {@code forEachLine} reads when it expands a directory: the regular files, links to regular files
     * included. A dangling or looping link has no lines to read and is left out, as {@code zip} leaves it out of an
     * archive and {@code sizeOf} out of a total, instead of failing the whole run part-way through when its file is
     * first opened; a special file - a FIFO, socket or device node - is left out as the copy family and {@code zip}
     * leave it out, because reading a FIFO blocks until a writer appears, which hung the whole run for ever (a
     * FIFO or device named as the source itself is still read, see {@code checkLineSource}).
     */
    private static final com.landawn.abacus.util.function.BiPredicate<File, File> readable_entries_filter = (parentDir, file) -> Files
            .isRegularFile(file.toPath());

    private static final com.landawn.abacus.util.function.BiPredicate<File, File> directories_only_filter = (parentDir, file) -> file.isDirectory();

    private IOUtil() {
        // no instance;
    }

    /**
     * Retrieves the host name of the local machine.
     *
     * <p>A successful result is cached for the lifetime of the JVM. Resolution is bounded by a timeout on
     * every platform and runs on a single shared daemon thread, so concurrent callers share one attempt and a
     * lookup that ignores interruption can neither accumulate threads nor keep the JVM alive. If the host name
     * cannot be determined, {@code "UNKNOWN_HOST_NAME"} is returned and the failure is retried after a short
     * backoff rather than cached permanently. Interruption is restored before returning.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String hostName = IOUtil.getHostName();
     * System.out.println("Current host name: " + hostName);
     * }</pre>
     *
     * @return the host name of the local machine, or "UNKNOWN_HOST_NAME" if it cannot be determined.
     */
    public static String getHostName() {
        String ret = hostName;

        if (ret != null) {
            return ret;
        }

        // This may be slow on some machines. It's resolved on first use rather than in a static initializer.
        final Future<String> future = submitHostNameResolutionIfAbsent();

        if (future == null) {
            // A recent attempt failed or timed out and the retry backoff has not elapsed yet.
            return UNKNOWN_HOST_NAME;
        }

        try {
            ret = future.get(HOST_NAME_RESOLVE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(e, "Interrupted while resolving the host name");

            // Leave the attempt in flight: it did not fail, and the next caller can still use its result.
            return UNKNOWN_HOST_NAME;
        } catch (final TimeoutException e) {
            logger.error(e, "Timed out resolving the host name after {} seconds", HOST_NAME_RESOLVE_TIMEOUT_SECONDS);

            // Deliberately not cancelled: a native name lookup may ignore interruption, so cancelling would
            // not free the resolver thread. The attempt stays in flight for a later caller to reuse, but the
            // backoff stops every later call from blocking for the full timeout all over again.
            backOffHostNameResolution();

            return UNKNOWN_HOST_NAME;
        } catch (final Exception e) {
            logger.error(e, "Failed to get host name");
            ret = null;
        }

        return completeHostNameResolution(ret);
    }

    /**
     * Returns the single in-flight host-name resolution, starting one if none is running.
     *
     * @return the shared {@code Future} - always when one has already completed, otherwise a newly started
     *         one - or {@code null} while the backoff from a recent failure or timeout has not elapsed.
     */
    private static Future<String> submitHostNameResolutionIfAbsent() {
        synchronized (hostNameLock) {
            // A finished attempt is handed over whatever the backoff says. The backoff exists so that a lookup
            // which is still hanging is not awaited all over again, and a Future that has already completed
            // cannot hang. Testing the backoff first meant a lookup that timed out at 5s but finished at 6s
            // was ignored for the remaining 59s, with the answer sitting in the Future the whole time.
            if (hostNameFuture != null && hostNameFuture.isDone()) {
                return hostNameFuture;
            }

            // Checked before the in-flight attempt: after a timeout that attempt is deliberately kept, but it
            // must not be awaited all over again on every subsequent call.
            if (System.currentTimeMillis() < hostNameRetryAtMillis) {
                return null;
            }

            if (hostNameFuture != null) {
                return hostNameFuture;
            }

            if (hostNameResolver == null) {
                hostNameResolver = Executors.newSingleThreadExecutor(r -> {
                    final Thread thread = new Thread(r, "abacus-hostname-resolver");
                    thread.setDaemon(true);
                    return thread;
                });
            }

            hostNameFuture = hostNameResolver.submit(() -> InetAddress.getLocalHost().getHostName());

            return hostNameFuture;
        }
    }

    /**
     * Records the outcome of a completed resolution attempt: caches a resolved name for the lifetime of the
     * JVM, or starts a retry backoff so a failing lookup is not repeated on every call.
     *
     * @param resolved the resolved host name, or {@code null}/empty if the attempt failed.
     * @return the host name, or {@code "UNKNOWN_HOST_NAME"} if it could not be determined.
     */
    private static String completeHostNameResolution(final String resolved) {
        synchronized (hostNameLock) {
            hostNameFuture = null;

            // Released on every outcome, not only on success: the executor's single worker used to stay parked
            // for the rest of the JVM's life after a failed lookup, since only the success path shut it down.
            releaseHostNameResolver();

            if (Strings.isEmpty(resolved)) {
                hostNameRetryAtMillis = System.currentTimeMillis() + HOST_NAME_RETRY_BACKOFF_MILLIS;

                return UNKNOWN_HOST_NAME;
            }

            hostName = resolved;

            return resolved;
        }
    }

    /**
     * Suppresses further host-name resolution attempts for a short period after an attempt timed out, so that
     * a lookup which never completes cannot make every subsequent call block for the full timeout.
     */
    private static void backOffHostNameResolution() {
        synchronized (hostNameLock) {
            hostNameRetryAtMillis = System.currentTimeMillis() + HOST_NAME_RETRY_BACKOFF_MILLIS;

            // The in-flight lookup is deliberately kept (see the caller), and shutdown() lets it run to completion;
            // releasing the executor now just means its thread goes away as soon as that lookup returns instead of
            // parking forever. A later attempt creates a fresh executor.
            releaseHostNameResolver();
        }
    }

    /** Shuts down the resolver executor, if any, so its worker thread is not kept for the lifetime of the JVM. Call under {@link #hostNameLock}. */
    private static void releaseHostNameResolver() {
        if (hostNameResolver != null) {
            hostNameResolver.shutdown();
            hostNameResolver = null;
        }
    }

    /**
     * Returns the free disk space on the volume where the current working directory resides, in kilobytes (KB).
     * This is equivalent to calling {@code freeDiskSpaceInKB(new File(".").getAbsolutePath())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     long freeSpace = IOUtil.freeDiskSpaceInKB();
     *     System.out.println("Free disk space: " + freeSpace + " KB");
     * } catch (UncheckedIOException e) {
     *     System.err.println("Failed to get free disk space: " + e.getMessage());
     * }
     * }</pre>
     *
     * @return the amount of free disk space in kilobytes.
     * @throws UncheckedIOException if the operating-system free-space query cannot be executed or its output cannot be read or interpreted.
     */
    public static long freeDiskSpaceInKB() throws UncheckedIOException {
        try {
            return FileSystemUtil.freeSpaceKb();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the free disk space on the volume where the current working directory resides, in kilobytes (KB).
     * This is equivalent to calling {@code freeDiskSpaceInKB(new File(".").getAbsolutePath(), timeout)}.
     * The command to retrieve the disk space will be aborted if it exceeds the specified timeout.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     long freeSpace = IOUtil.freeDiskSpaceInKB(5000);   // returns free disk space in KB (5000 = 5s timeout)
     *     System.out.println("Free disk space: " + freeSpace + " KB");
     * } catch (UncheckedIOException e) {
     *     System.err.println("Failed to get free disk space within the specified timeout: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param timeout the maximum time in milliseconds to wait for the command to complete. A value of zero or less means no timeout.
     * @return the amount of free disk space in kilobytes.
     * @throws UncheckedIOException if the operating-system free-space query cannot be executed or its output cannot be read or interpreted or
     *         the command times out.
     */
    public static long freeDiskSpaceInKB(final long timeout) throws UncheckedIOException {
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
     *     long freeSpace = IOUtil.freeDiskSpaceInKB("C:\\");
     *     System.out.println("Free space on C: " + freeSpace + " KB");
     *
     *     // For Unix-like systems
     *     long freeSpaceUnix = IOUtil.freeDiskSpaceInKB("/home");
     *     System.out.println("Free space on /home: " + freeSpaceUnix + " KB");
     * } catch (UncheckedIOException e) {
     *     System.err.println("Failed to get free disk space for the specified path: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param path the path to a file or directory on the volume to check. It must not be {@code null}. On Unix, it should not be an empty string.
     * @return the amount of free disk space in kilobytes.
     * @throws IllegalArgumentException if {@code FileSystemUtil} rejects the path. This method adds no
     *         validation of its own, so the exact condition is that class's.
     * @throws UncheckedIOException if the operating-system free-space query cannot be executed or its output cannot be read or interpreted.
     */
    public static long freeDiskSpaceInKB(final String path) throws IllegalArgumentException, UncheckedIOException {
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
     *     long freeSpace = IOUtil.freeDiskSpaceInKB("C:\\", 5000);
     *     System.out.println("Free space on C: " + freeSpace + " KB");
     * } catch (UncheckedIOException e) {
     *     System.err.println("Failed to get free disk space for the specified path within the timeout: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param path the path to a file or directory on the volume to check. It must not be {@code null}. On Unix, it should not be an empty string.
     * @param timeout the maximum time in milliseconds to wait for the command to complete. A value of zero or less means no timeout.
     * @return the amount of free disk space in kilobytes.
     * @throws IllegalArgumentException if {@code FileSystemUtil} rejects the path. This method adds no
     *         validation of its own, so the exact condition is that class's.
     * @throws UncheckedIOException if the operating-system free-space query cannot be executed or its output cannot be read or interpreted or
     *         the command times out.
     */
    public static long freeDiskSpaceInKB(final String path, final long timeout) throws IllegalArgumentException, UncheckedIOException {
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
     * Resolves a charset <i>name</i> the same way {@link #checkCharset(Charset)} resolves a {@link Charset}:
     * {@code null} or empty means this class's default (UTF-8). Keeps the {@code String}-named overloads in
     * step with the {@code Charset} ones, which all document "if {@code null}, UTF-8".
     *
     * @param charsetName the charset name; {@code null} or empty means UTF-8.
     * @return the resolved charset.
     */
    static Charset checkCharset(final String charsetName) {
        return Strings.isEmpty(charsetName) ? DEFAULT_CHARSET : Charsets.get(charsetName);
    }

    /**
     * Converts a character array to a byte array using the default charset (UTF-8).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = new char[] {'H', 'e', 'l', 'l', 'o'};
     * byte[] bytes = IOUtil.charsToBytes(chars);
     * }</pre>
     *
     * @param chars the character array to convert. May be {@code null} or empty.
     * @return the resulting byte array, or an empty byte array if the input is {@code null} or empty.
     */
    public static byte[] charsToBytes(final char[] chars) {
        return charsToBytes(chars, DEFAULT_CHARSET);
    }

    /**
     * Converts a character array to a byte array using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = new char[] {'H', 'e', 'l', 'l', 'o'};
     * byte[] bytes = IOUtil.charsToBytes(chars, StandardCharsets.UTF_8);
     * IOUtil.charsToBytes(null, StandardCharsets.UTF_8);   // returns empty byte array
     * }</pre>
     *
     * @param chars the character array to convert. May be {@code null} or empty.
     * @param charset the character set to use for encoding. If {@code null}, the default charset (UTF-8) is used.
     * @return the resulting byte array, or an empty byte array if the input is {@code null} or empty.
     */
    public static byte[] charsToBytes(final char[] chars, final Charset charset) {
        if (N.isEmpty(chars)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return charsToBytes(chars, 0, chars.length, charset);
    }

    /**
     * Converts a sub-array of a character array to a byte array using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = new char[] {'H', 'e', 'l', 'l', 'o'};
     * // Convert the sub-array {'e', 'l', 'l'}
     * byte[] bytes = IOUtil.charsToBytes(chars, 1, 3, StandardCharsets.UTF_8);
     *
     * // A slice that splits a surrogate pair encodes as the replacement byte, not as half a character:
     * char[] emoji = "ab😀".toCharArray();          // 'a', 'b', high surrogate, low surrogate
     * IOUtil.charsToBytes(emoji, 2, 1, StandardCharsets.UTF_8);   // returns {(byte) '?'}
     * }</pre>
     *
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code charCount} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate that results
     * cannot be encoded and is replaced by the charset's replacement byte (typically {@code '?'}) without any
     * error. Slice on code-point boundaries - for example with {@link String#offsetByCodePoints(int, int)} - when
     * that matters.
     *
     * @param chars the source character array; {@code null} is accepted only when {@code offset} is 0 and {@code charCount} is 0.
     * @param offset the starting position in the character array (0-based), must be &gt;= 0.
     * @param charCount the number of characters to convert, must be &gt;= 0.
     * @param charset the character set to use for encoding. If {@code null}, the default charset (UTF-8) is used.
     * @return the resulting byte array, or an empty byte array if {@code charCount} is zero.
     * @throws IllegalArgumentException if {@code offset} or {@code charCount} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} or {@code charCount} is out of bounds.
     */
    public static byte[] charsToBytes(final char[] chars, final int offset, final int charCount, final Charset charset)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(charCount, cs.count);
        N.checkFromIndexSize(offset, charCount, N.len(chars));

        if (charCount == 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return new String(chars, offset, charCount).getBytes(checkCharset(charset));
    }

    /**
     * Converts a byte array to a character array using the default charset (UTF-8).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = new byte[] {72, 101, 108, 108, 111};
     * char[] chars = IOUtil.bytesToChars(bytes);
     * IOUtil.bytesToChars(null);   // returns empty char array
     * }</pre>
     *
     * @param bytes the byte array to convert. May be {@code null} or empty.
     * @return the resulting character array, or an empty character array if the input is {@code null} or empty.
     */
    public static char[] bytesToChars(final byte[] bytes) {
        return bytesToChars(bytes, DEFAULT_CHARSET);
    }

    /**
     * Converts a byte array to a character array using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = new byte[] {72, 101, 108, 108, 111};
     * char[] chars = IOUtil.bytesToChars(bytes, StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param bytes the byte array to convert. May be {@code null} or empty.
     * @param charset the character set to use for decoding. If {@code null}, the default charset (UTF-8) is used.
     * @return the resulting character array, or an empty character array if the input is {@code null} or empty.
     */
    public static char[] bytesToChars(final byte[] bytes, final Charset charset) {
        if (N.isEmpty(bytes)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return bytesToChars(bytes, 0, bytes.length, charset);
    }

    /**
     * Converts a sub-array of a byte array to a character array using the specified character set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = new byte[] {72, 101, 108, 108, 111};
     * // Convert the sub-array {101, 108, 108}
     * char[] chars = IOUtil.bytesToChars(bytes, 1, 3, StandardCharsets.UTF_8);
     * }</pre>
     *
     * @param bytes the source byte array; {@code null} is accepted only when {@code offset} is 0 and {@code byteCount} is 0.
     * @param offset the starting position in the byte array (0-based), must be &gt;= 0.
     * @param byteCount the number of bytes to convert, must be &gt;= 0.
     * @param charset the character set to use for decoding. If {@code null}, the default charset (UTF-8) is used.
     * @return the resulting character array, or an empty character array if {@code byteCount} is zero. A slice that
     *         starts or ends inside a multi-byte sequence decodes the cut sequence to {@code U+FFFD} (the decoder
     *         replaces, it does not throw), the mirror of what {@link #charsToBytes(char[], int, int, Charset)} does
     *         to a split surrogate pair.
     * @throws IllegalArgumentException if {@code offset} or {@code byteCount} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} or {@code byteCount} is out of bounds.
     */
    public static char[] bytesToChars(final byte[] bytes, final int offset, final int byteCount, final Charset charset)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(byteCount, cs.count);
        N.checkFromIndexSize(offset, byteCount, N.len(bytes));

        if (byteCount == 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return new String(bytes, offset, byteCount, checkCharset(charset)).toCharArray();
    }

    /**
     * Converts a {@code String} to an {@code InputStream} using the default charset (UTF-8).
     * If the input string is {@code null} or empty, an empty {@code InputStream} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String str = "Hello, World!";
     * try (InputStream inputStream = IOUtil.stringToInputStream(str)) {
     *     // Use the inputStream
     * }
     * }</pre>
     *
     * @param str the string to convert. May be {@code null}.
     * @return an {@code InputStream} for the given string, or an empty {@code InputStream} if the input is {@code null} or empty.
     */
    public static InputStream stringToInputStream(final String str) {
        return stringToInputStream(str, DEFAULT_CHARSET);
    }

    /**
     * Converts a {@code String} to an {@code InputStream} using the specified character set.
     * If the input string is {@code null} or empty, an empty {@code InputStream} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String str = "Hello, World!";
     * try (InputStream inputStream = IOUtil.stringToInputStream(str, StandardCharsets.UTF_8)) {
     *     // Use the inputStream
     * }
     * }</pre>
     *
     * @param str the string to convert. May be {@code null}.
     * @param charset the character set to use for encoding. If {@code null}, the default charset (UTF-8) is used.
     * @return an {@code InputStream} for the given string, or an empty {@code InputStream} if the input is {@code null} or empty.
     */
    public static InputStream stringToInputStream(final String str, final Charset charset) {
        // Encode straight from the String: str.toCharArray() -> charsToBytes(..) would copy the characters out
        // and then copy them back into a temporary String just to call the same String.getBytes(Charset).
        final byte[] bytes = N.isEmpty(str) ? N.EMPTY_BYTE_ARRAY : str.getBytes(checkCharset(charset));

        return new ByteArrayInputStream(bytes);
    }

    /**
     * Converts a {@code String} into a {@code Reader}.
     * If the input string is {@code null}, an empty reader is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String str = "Hello, World!";
     * try (Reader reader = IOUtil.stringToReader(str)) {
     *     // Use the reader
     * }
     * }</pre>
     *
     * @param str the string to convert. May be {@code null}.
     * @return a {@code Reader} for the given string, or an empty reader if the input is {@code null}.
     * @see StringReader
     */
    public static Reader stringToReader(final String str) {
        return new StringReader(Strings.nullToEmpty(str));
    }

    /**
     * Reads all bytes from a file into a byte array.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from, must not be {@code null}.
     * @return a byte array containing all bytes from the file.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into a byte array.
     */
    public static byte[] readAllBytes(final File source) throws IllegalArgumentException, UncheckedIOException {
        try {
            return withOpenedFile(source, is -> readBytes(is, 0, Long.MAX_VALUE));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
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
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @return a byte array containing all bytes read from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the stream is too large to be read into a byte array.
     */
    public static byte[] readAllBytes(final InputStream source) throws IllegalArgumentException, UncheckedIOException {
        try {
            return readBytes(source, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads up to a specified number of bytes from a file into a byte array, starting from a given offset.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * try {
     *     // Read 1024 bytes, starting from the 100th byte
     *     byte[] partialBytes = IOUtil.readBytes(file, 100, 1024);
     *     // Process the partial bytes
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}.
     * @param offset the starting position in bytes from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of bytes to read, must be &gt;= 0.
     * @return a byte array containing the bytes read from the file. The length of the array will be at most {@code maxLen}.
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative, or if {@code source} is {@code null} or is a directory
     *         rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails while locating or reading
     *         the requested range
     */
    public static byte[] readBytes(final File source, final long offset, final int maxLen) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        try {
            return withOpenedFile(source, is -> readBytes(is, offset, maxLen));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
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
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param offset the starting position in bytes from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of bytes to read, must be &gt;= 0. When 0, the method returns
     *               an empty array immediately without skipping any bytes.
     * @return a byte array containing the bytes read from the stream. The length of the array will be at most
     *         {@code maxLen}. If the stream holds fewer than {@code offset} bytes, an empty array is returned -
     *         indistinguishable from a stream that had exactly {@code offset} bytes and nothing after them.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code maxLen} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range.
     */
    public static byte[] readBytes(final InputStream source, final long offset, final int maxLen) throws IllegalArgumentException, UncheckedIOException {
        try {
            return readBytes(source, offset, (long) maxLen);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @throws IllegalArgumentException if {@code source} is {@code null}, or {@code offset} or {@code maxLen} is negative.
     * @throws IOException if skipping or reading the input fails.
     */
    private static byte[] readBytes(final InputStream source, final long offset, final long maxLen) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
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
                if (cnt == 0) {
                    break;
                }

                count += cnt;

                if (count < maxLen && count >= arrayLength) {
                    // Grow by 1.75x. The multiplication is done in floating point and kept in a long so that it
                    // cannot wrap the way `arrayLength * 7 / 4` would once arrayLength passes Integer.MAX_VALUE/7.
                    final long newCapacityLong = (long) (arrayLength * 1.75);
                    final int newCapacity;

                    if (newCapacityLong > maxLen || newCapacityLong > N.MAX_ARRAY_SIZE) {
                        newCapacity = (int) N.min(maxLen, N.MAX_ARRAY_SIZE);
                    } else {
                        newCapacity = (int) newCapacityLong;
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
     * Reads all characters from a file into a character array using the default charset (UTF-8).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from, must not be {@code null}.
     * @return a character array containing all characters from the file.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into a character array.
     */
    public static char[] readAllChars(final File source) throws IllegalArgumentException, UncheckedIOException {
        return readAllChars(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all characters from a file into a character array using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @return a character array containing all characters from the file.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into a character array.
     */
    public static char[] readAllChars(final File source, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        try {
            return withOpenedFile(source, is -> readAllChars(is, charset));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads all remaining characters from an {@code InputStream} into a character array using the default charset (UTF-8).
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
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @return a character array containing all characters read from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the stream is too large to be read into a character array.
     */
    public static char[] readAllChars(final InputStream source) throws IllegalArgumentException, UncheckedIOException {
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
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @return a character array containing all characters read from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the stream is too large to be read into a character array.
     */
    public static char[] readAllChars(final InputStream source, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        // Validated here, not left to newInputStreamReader below, so a null stream is reported as 'source' -
        // the caller's own argument name, and the one this javadoc names - exactly as the readAllBytes twin
        // and every Reader overload of this family already report it.
        N.checkArgNotNull(source, cs.source);

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
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @return a character array containing all characters read from the reader.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the reader's content is too large to be read into a character array.
     */
    public static char[] readAllChars(final Reader source) throws IllegalArgumentException, UncheckedIOException {
        try {
            return readChars(source, 0, Long.MAX_VALUE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads up to a specified number of characters from a file into a character array, starting from a given character offset, using the default charset (UTF-8).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 1024 characters, starting from the 100th character
     *     char[] partialChars = IOUtil.readChars(file, 100, 1024);
     *     // Process the partial characters
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the file to read from, must not be {@code null}.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a character array containing the characters read from the file. The length of the array will be at most {@code maxLen}.
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative, or if {@code source} is {@code null} or is a directory
     *         rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails while locating or reading
     *         the requested range
     */
    public static char[] readChars(final File source, final long offset, final int maxLen) throws IllegalArgumentException, UncheckedIOException {
        return readChars(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads up to a specified number of characters from a file into a character array, starting from a given character offset, using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 1024 characters, starting from the 100th character, using UTF-8 encoding
     *     char[] partialChars = IOUtil.readChars(file, StandardCharsets.UTF_8, 100, 1024);
     *     // Process the partial characters
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the file to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a character array containing the characters read from the file. The length of the array will be at most {@code maxLen}.
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative, or if {@code source} is {@code null} or is a directory
     *         rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails while locating or reading
     *         the requested range
     */
    public static char[] readChars(final File source, final Charset charset, final long offset, final int maxLen)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        try {
            return withOpenedFile(source, is -> readChars(is, charset, offset, maxLen));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads up to a specified number of characters from an {@code InputStream} into a character array, starting from a given character offset, using the default charset (UTF-8).
     * This method will skip the specified number of characters from the stream before starting to read.
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
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a character array containing the characters read from the stream. The length of the array will be at most {@code maxLen}.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code maxLen} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range.
     * @see #readChars(InputStream, Charset, long, int)
     * @see #readChars(Reader, long, int)
     */
    public static char[] readChars(final InputStream source, final long offset, final int maxLen) throws IllegalArgumentException, UncheckedIOException {
        return readChars(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads up to a specified number of characters from an {@code InputStream} into a character array, starting from a given character offset, using the specified character set.
     * This method will skip the specified number of characters from the stream before starting to read.
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
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a character array containing the characters read from the stream. The length of the array will be at most {@code maxLen}.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code maxLen} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range.
     * @see #readChars(Reader, long, int)
     */
    public static char[] readChars(final InputStream source, final Charset charset, final long offset, final int maxLen)
            throws IllegalArgumentException, UncheckedIOException {
        // Before the offset/maxLen checks, as the readBytes twin does, so a call that is wrong twice over
        // reports the same argument on both paths.
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        // The decoder is deliberately not closed: closing it would close the caller-owned stream.
        return readChars(createReader(source, charset), offset, maxLen);
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
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0. When 0, the method returns
     *               an empty array immediately without skipping any characters.
     * @return a character array containing the characters read from the reader. The length of the array will be at
     *         most {@code maxLen}. If the reader holds fewer than {@code offset} characters, an empty array is
     *         returned - indistinguishable from a reader that had exactly {@code offset} characters and nothing after
     *         them.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code maxLen} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range.
     */
    public static char[] readChars(final Reader source, final long offset, final int maxLen) throws IllegalArgumentException, UncheckedIOException {
        try {
            return readChars(source, offset, (long) maxLen);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @throws IllegalArgumentException if {@code source} is {@code null}, or {@code offset} or {@code maxLen} is negative.
     * @throws IOException if skipping or reading the input fails.
     */
    private static char[] readChars(final Reader source, final long offset, final long maxLen) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
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
                if (cnt == 0) {
                    break;
                }

                count += cnt;

                if (count < maxLen && count >= arrayLength) {
                    // Grow by 1.75x. The multiplication is done in floating point and kept in a long so that it
                    // cannot wrap the way `arrayLength * 7 / 4` would once arrayLength passes Integer.MAX_VALUE/7.
                    final long newCapacityLong = (long) (arrayLength * 1.75);
                    final int newCapacity;

                    if (newCapacityLong > maxLen || newCapacityLong > N.MAX_ARRAY_SIZE) {
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
     * Reads the entire content of a file into a {@code String} using the default charset (UTF-8).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from. It can be a regular file, gzipped file (.gz), and zip file (.zip, reading the first non-directory entry).
     * @return a {@code String} containing the entire content of the file.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into a string.
     */
    public static String readAllToString(final File source) throws IllegalArgumentException, UncheckedIOException {
        return readAllToString(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the entire content of a file into a {@code String} using the specified character set name.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from. It can be a regular file, gzipped file (.gz), and zip file (.zip, reading the first non-directory entry).
     * @param encoding the name of the character set to use for decoding. If {@code null} or empty, the default
     *        charset (UTF-8) is used, matching every {@code Charset}-taking overload in this class.
     * @return a {@code String} containing the entire content of the file.
     * @throws IllegalArgumentException if {@code source} is {@code null} or is a directory rather than a file.
     * @throws IllegalCharsetNameException if {@code encoding} is not a legal charset name (unchecked exception).
     * @throws UnsupportedCharsetException if the named charset is not available in this JVM (unchecked exception).
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into a string.
     */
    public static String readAllToString(final File source, final String encoding)
            throws IllegalArgumentException, IllegalCharsetNameException, UnsupportedCharsetException, UncheckedIOException {
        return readAllToString(source, checkCharset(encoding));
    }

    /**
     * Reads the entire content of a file into a {@code String} using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from. It can be a regular file, gzipped file (.gz), and zip file (.zip, reading the first non-directory entry).
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @return a {@code String} containing the entire content of the file.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into a string.
     */
    public static String readAllToString(final File source, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        try {
            return withOpenedFile(source, is -> readAllToString(is, charset));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads all remaining characters from an {@code InputStream} into a {@code String} using the default charset (UTF-8).
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
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @return a {@code String} containing all content read from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the stream is too large to be read into a string.
     * @see #readAllToString(InputStream, Charset)
     * @see #readAllToString(Reader)
     */
    public static String readAllToString(final InputStream source) throws IllegalArgumentException, UncheckedIOException {
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
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @return a {@code String} containing all content read from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the stream is too large to be read into a string.
     * @see #readAllToString(Reader)
     */
    public static String readAllToString(final InputStream source, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        // Decoding the whole byte[] in one call is faster than streaming through an InputStreamReader, at the
        // cost of holding the bytes and the String at the same time - hence the size warning above.
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
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @return a {@code String} containing all content read from the reader.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the reader's content is too large to be read into a string.
     */
    public static String readAllToString(final Reader source) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);

        // Accumulated directly instead of via readAllChars(..): that builds the whole content as a char[]
        // (grown, then trimmed into an exact-size copy) which String.valueOf(..) then copies a second time -
        // three passes over a size this method's own javadoc already warns about, where two will do.
        final StringBuilder sb = Objectory.createStringBuilder();
        final char[] buf = Objectory.createCharArrayBuffer();

        try {
            int cnt = 0;

            while (EOF != (cnt = read(source, buf, 0, buf.length))) {
                if (cnt == 0) {
                    break;
                }

                sb.append(buf, 0, cnt);
            }

            return sb.toString();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buf);
            Objectory.recycle(sb);
        }
    }

    /**
     * Reads up to a specified number of characters from a file into a {@code String}, starting from a given character offset, using the default charset (UTF-8).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 1024 characters, starting from the 100th character
     *     String partialContent = IOUtil.readToString(file, 100, 1024);
     *     System.out.println(partialContent);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the file to read from. It can be a regular file, gzipped file (.gz), and zip file (.zip, reading the first non-directory entry).
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a {@code String} containing the characters read from the file.
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative, or if {@code source} is {@code null} or is a directory
     *         rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails while locating or reading
     *         the requested range
     */
    public static String readToString(final File source, final long offset, final int maxLen) throws IllegalArgumentException, UncheckedIOException {
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
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading partial file content: " + e.getMessage());
     * }
     * }</pre>
     *
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the file to read from. It can be a regular file, gzipped file (.gz), and zip file (.zip, reading the first non-directory entry).
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a {@code String} containing the characters read from the file.
     * @throws IllegalArgumentException if {@code offset} or {@code maxLen} is negative, or if {@code source} is {@code null} or is a directory
     *         rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails while locating or reading
     *         the requested range
     */
    public static String readToString(final File source, final Charset charset, final long offset, final int maxLen)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        try {
            return withOpenedFile(source, is -> readToString(is, charset, offset, maxLen));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads up to a specified number of characters from an {@code InputStream} into a {@code String}, starting from a given character offset, using the default charset (UTF-8).
     * This method will skip the specified number of characters from the stream before starting to read.
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
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a {@code String} containing the characters read from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code maxLen} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range.
     * @see #readToString(InputStream, Charset, long, int)
     * @see #readToString(Reader, long, int)
     */
    public static String readToString(final InputStream source, final long offset, final int maxLen) throws IllegalArgumentException, UncheckedIOException {
        return readToString(source, DEFAULT_CHARSET, offset, maxLen);
    }

    /**
     * Reads up to a specified number of characters from an {@code InputStream} into a {@code String}, starting from a given character offset, using the specified character set.
     * This method will skip the specified number of characters from the stream before starting to read.
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
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a {@code String} containing the characters read from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code maxLen} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range.
     * @see #readToString(Reader, long, int)
     */
    public static String readToString(final InputStream source, final Charset charset, final long offset, final int maxLen)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(maxLen, cs.maxLen);

        // The decoder is deliberately not closed: closing it would close the caller-owned stream.
        return readToString(createReader(source, charset), offset, maxLen);
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
     * <p><b>Surrogate pairs:</b> {@code offset} and {@code maxLen} count {@code char} values, not code points,
     * so the requested range can end between the two halves of a surrogate pair. The lone surrogate is kept in
     * the result but becomes the charset's replacement character once it is encoded.
     *
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param maxLen the maximum number of characters to read, must be &gt;= 0.
     * @return a {@code String} containing the characters read from the reader.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code maxLen} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range.
     */
    public static String readToString(final Reader source, final long offset, final int maxLen) throws IllegalArgumentException, UncheckedIOException {
        final char[] chs = readChars(source, offset, maxLen);

        return String.valueOf(chs);
    }

    /**
     * Reads all lines from a file into a list of strings, using the default charset (UTF-8).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from. It can be a regular file, gzipped file (.gz), and zip file (.zip, reading the first non-directory entry).
     * @return a list of strings, each representing a line in the file.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into memory.
     */
    public static List<String> readAllLines(final File source) throws IllegalArgumentException, UncheckedIOException {
        return readAllLines(source, DEFAULT_CHARSET);
    }

    /**
     * Reads all lines from a file into a list of strings, using the specified character set name.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from. It can be a regular file, gzipped file (.gz), and zip file (.zip, reading the first non-directory entry).
     * @param encoding the name of the character set to use for decoding. If {@code null} or empty, the default
     *        charset (UTF-8) is used, matching every {@code Charset}-taking overload in this class.
     * @return a list of strings, each representing a line in the file.
     * @throws IllegalArgumentException if {@code source} is {@code null} or is a directory rather than a file.
     * @throws IllegalCharsetNameException if {@code encoding} is not a legal charset name (unchecked exception).
     * @throws UnsupportedCharsetException if the named charset is not available in this JVM (unchecked exception).
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into memory.
     */
    public static List<String> readAllLines(final File source, final String encoding)
            throws IllegalArgumentException, IllegalCharsetNameException, UnsupportedCharsetException, UncheckedIOException {
        return readAllLines(source, checkCharset(encoding));
    }

    /**
     * Reads all lines from a file into a list of strings, using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from. It can be a regular file, gzipped file (.gz), and zip file (.zip, reading the first non-directory entry).
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @return a list of strings, each representing a line in the file.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     * @throws OutOfMemoryError if the file is too large to be read into memory.
     */
    public static List<String> readAllLines(final File source, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        try {
            return withOpenedFile(source, is -> readAllLines(is, charset));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads all lines from an {@code InputStream} into a list of strings, using the default charset (UTF-8).
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
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @return a list of strings, each representing a line from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the stream is too large to be read into memory.
     * @see #readAllLines(InputStream, Charset)
     * @see #readAllLines(Reader)
     */
    public static List<String> readAllLines(final InputStream source) throws IllegalArgumentException, UncheckedIOException {
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
     * } catch (IOException e) {
     *     System.err.println("Error reading from stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @return a list of strings, each representing a line from the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the stream is too large to be read into memory.
     * @see #readAllLines(Reader)
     */
    public static List<String> readAllLines(final InputStream source, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);

        // The decoder is deliberately not closed: closing it would close the caller-owned stream.
        return readAllLines(createReader(source, charset));
    }

    /**
     * Reads all lines from a {@code Reader} into a list of strings.
     * <p>
     * Note: This method should not be used for readers with content size close to {@code Integer.MAX_VALUE} due to memory constraints.
     * The input reader is not closed by this method.
     *
     * <p><b>Reader position:</b> this reads to end of input, so there is nothing left to continue from.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     List<String> lines = IOUtil.readAllLines(reader);
     *     lines.forEach(System.out::println);
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @return a list of strings, each representing a line from the reader.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     * @throws OutOfMemoryError if the reader's content is too large to be read into memory.
     */
    public static List<String> readAllLines(final Reader source) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);

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
     * Reads a specified number of lines from a file into a list of strings, starting from a given line offset, using the default charset (UTF-8).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 10 lines, starting from the 5th line
     *     List<String> lines = IOUtil.readLines(file, 4, 10);
     *     lines.forEach(System.out::println);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading lines from file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}.
     * @param offset the 0-based index of the first line to read, must be &gt;= 0.
     * @param count the number of lines to read, must be &gt;= 0.
     * @return a list of strings, each representing a line from the specified range in the file.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code source} is {@code null} or is a directory
     *         rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails while locating or reading
     *         the requested range
     */
    public static List<String> readLines(final File source, final int offset, final int count) throws IllegalArgumentException, UncheckedIOException {
        return readLines(source, DEFAULT_CHARSET, offset, count);
    }

    /**
     * Reads a specified number of lines from a file into a list of strings, starting from a given line offset, using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     // Read 10 lines, starting from the 5th line, using UTF-8 encoding
     *     List<String> lines = IOUtil.readLines(file, StandardCharsets.UTF_8, 4, 10);
     *     lines.forEach(System.out::println);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading lines from file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param offset the 0-based index of the first line to read, must be &gt;= 0.
     * @param count the number of lines to read, must be &gt;= 0.
     * @return a list of strings, each representing a line from the specified range in the file.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code source} is {@code null} or is a directory
     *         rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails while locating or reading
     *         the requested range
     */
    public static List<String> readLines(final File source, final Charset charset, final int offset, final int count)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        try {
            return withOpenedFile(source, is -> readLines(is, charset, offset, count));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads a specified number of lines from an {@code InputStream} into a list of strings, starting from a given line offset, using the default charset (UTF-8).
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
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param offset the 0-based index of the first line to read, must be &gt;= 0.
     * @param count the number of lines to read, must be &gt;= 0.
     * @return a list of strings, each representing a line from the specified range in the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range
     * @see #readLines(InputStream, Charset, int, int)
     * @see #readLines(Reader, int, int)
     */
    public static List<String> readLines(final InputStream source, final int offset, final int count) throws IllegalArgumentException, UncheckedIOException {
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
     * @param source the {@code InputStream} to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param offset the 0-based index of the first line to read, must be &gt;= 0.
     * @param count the number of lines to read, must be &gt;= 0.
     * @return a list of strings, each representing a line from the specified range in the stream.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range
     * @see #readLines(Reader, int, int)
     */
    public static List<String> readLines(final InputStream source, final Charset charset, final int offset, final int count)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        // The decoder is created here and discarded, so the Reader overload's exact - and therefore
        // character-at-a-time - path would be pure cost. How much of the caller's stream is consumed is
        // unspecified either way; see the class contract.
        return withPooledBufferedReader(createReader(source, charset), br -> readLines(br, offset, count));
    }

    /**
     * Reads a specified number of lines from a {@code Reader} into a list of strings, starting from a given line offset.
     * The input reader is not closed by this method.
     *
     * <p><b>Reader position:</b> the reader is left immediately after the last line returned, so the caller can
     * carry on reading from it. Exactness costs a character-at-a-time read, plus one character of look-ahead to
     * tell a lone {@code '\r'} from the first half of {@code "\r\n"}; a reader supporting
     * {@link Reader#mark(int)} is put back exactly (the look-ahead sets a mark of its own, so a mark the caller
     * had set before the call no longer holds), and for one that does not, only a lone {@code '\r'} ending
     * the last line read keeps a character the caller would otherwise still see. A source that is already a
     * {@link java.io.BufferedReader} is read through directly, at full speed and exactly. Measured on 200,000
     * lines, taking a bounded slice is free; reading <i>every</i> line this way costs about 3.5x what
     * {@link #readAllLines(Reader)} does, so prefer that when the whole reader is wanted - it consumes the
     * reader either way, so there is no exactness to trade.
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
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @param offset the 0-based index of the first line to read, must be &gt;= 0.
     * @param count the number of lines to read, must be &gt;= 0. When 0, an empty list is returned immediately and
     *              the reader is not touched - the {@code offset} lines are not consumed - matching
     *              {@link #readChars(Reader, long, int)}.
     * @return a list of strings, each representing a line from the specified range in the reader.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws UncheckedIOException if reading from {@code source} fails while locating or reading the requested range
     */
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static List<String> readLines(final Reader source, int offset, int count) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        final List<String> res = new ArrayList<>();

        // A count of 0 asks for nothing and leaves the reader where it was - the offset lines are NOT consumed -
        // matching readChars(reader, offset, 0) and the write(.., offset, 0, ..) forms. This was the one slicer
        // that still skipped the offset first and only then returned nothing.
        if (count == 0) {
            return res;
        }

        // Reads exactly as far as the lines it returns. Wrapping a caller-owned reader in a pooled buffer and
        // then discarding that buffer used to leave the reader drained, so a caller who asked for one line got
        // it and lost the rest. See ExactLineReader.
        final ExactLineReader lineReader = new ExactLineReader(source);

        try {
            while (offset-- > 0 && lineReader.readLine() != null) { //NOSONAR
                // continue
            }

            String line = null;

            while (count-- > 0 && (line = lineReader.readLine()) != null) { //NOSONAR
                res.add(line);
            }

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lineReader.recycle();
        }

        return res;
    }

    /**
     * Reads the first line from a file using the default charset (UTF-8).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String firstLine = IOUtil.readFirstLine(file);
     *     if (firstLine != null) {
     *         System.out.println("First line: " + firstLine);
     *     }
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}.
     * @return the first line of the file, or {@code null} if the file is empty.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     */
    @MayReturnNull
    public static String readFirstLine(final File source) throws IllegalArgumentException, UncheckedIOException {
        return readFirstLine(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the first line from a file using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String firstLine = IOUtil.readFirstLine(file, StandardCharsets.UTF_8);
     *     if (firstLine != null) {
     *         System.out.println("First line: " + firstLine);
     *     }
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @return the first line of the file, or {@code null} if the file is empty.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     */
    @MayReturnNull
    public static String readFirstLine(final File source, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        try {
            // Buffered: the reader is opened and discarded here, so nothing can observe how far it was read
            // and the Reader overload's character-at-a-time path would be pure cost.
            return withOpenedFile(source, is -> withPooledBufferedReader(createReader(is, charset), br -> readFirstLine(br)));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads the first line from a {@code Reader}.
     * The input reader is not closed by this method.
     *
     * <p><b>Reader position:</b> the reader is left immediately after the last line returned, so the caller can
     * carry on reading from it. Exactness costs a character-at-a-time read, plus one character of look-ahead to
     * tell a lone {@code '\r'} from the first half of {@code "\r\n"}; a reader supporting
     * {@link Reader#mark(int)} is put back exactly (the look-ahead sets a mark of its own, so a mark the caller
     * had set before the call no longer holds), and for one that does not, only a lone {@code '\r'} ending
     * the last line read keeps a character the caller would otherwise still see. A source that is already a
     * {@link java.io.BufferedReader} is read through directly, at full speed and exactly.
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
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @return the first line from the reader, or {@code null} if the reader is empty.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     */
    @MayReturnNull
    public static String readFirstLine(final Reader source) throws IllegalArgumentException, UncheckedIOException {
        return readLine(source, 0);
    }

    /**
     * Reads the last line from a file using the default charset (UTF-8).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String lastLine = IOUtil.readLastLine(file);
     *     if (lastLine != null) {
     *         System.out.println("Last line: " + lastLine);
     *     }
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}.
     * @return the last line of the file, or {@code null} if the file is empty.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     */
    @MayReturnNull
    public static String readLastLine(final File source) throws IllegalArgumentException, UncheckedIOException {
        return readLastLine(source, DEFAULT_CHARSET);
    }

    /**
     * Reads the last line from a file using the specified character set.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String lastLine = IOUtil.readLastLine(file, StandardCharsets.UTF_8);
     *     if (lastLine != null) {
     *         System.out.println("Last line: " + lastLine);
     *     }
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @return the last line of the file, or {@code null} if the file is empty.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     */
    @MayReturnNull
    public static String readLastLine(final File source, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        try {
            return withOpenedFile(source, is -> readLastLine(createReader(is, charset)));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads the last line from a {@code Reader} and returns it as a {@code String}.
     * The input reader is not closed by this method.
     *
     * <p><b>Reader position:</b> this reads to end of input, so there is nothing left to continue from.
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
     * @param source the {@code Reader} to read the last line from, must not be {@code null}.
     * @return a {@code String} containing the last line from the reader, or {@code null} if the reader is empty.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if reading from {@code source} fails
     */
    @MayReturnNull
    public static String readLastLine(final Reader source) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);

        final boolean isBufferedReader = IOUtil.isBufferedReader(source);
        final BufferedReader br = isBufferedReader ? (BufferedReader) source : Objectory.createBufferedReader(source); //NOSONAR

        try {
            String ret = null;
            String line = null;

            while ((line = br.readLine()) != null) { //NOSONAR
                ret = line;
            }

            return ret;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (!isBufferedReader) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Reads a specific line from a file using the default charset and returns it as a {@code String}.
     * The line to read is determined by the provided line index (0-based).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String line = IOUtil.readLine(file, 0);   // Read the first line
     *     System.out.println("First line: " + line);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read the line from, must not be {@code null}.
     * @param lineIndex the index of the line to read, starting from 0 for the first line.
     * @return a {@code String} containing the specified line from the file, or {@code null} if the file has fewer lines.
     * @throws IllegalArgumentException if the line index is negative, or if {@code source} is {@code null} or is a directory rather than a file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     */
    @MayReturnNull
    public static String readLine(final File source, final int lineIndex) throws IllegalArgumentException, UncheckedIOException {
        return readLine(source, DEFAULT_CHARSET, lineIndex);
    }

    /**
     * Reads a specific line from a file using the specified character set and returns it as a {@code String}.
     * The line to read is determined by the provided line index (0-based).
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * try {
     *     String line = IOUtil.readLine(file, StandardCharsets.UTF_8, 5);   // Read the 6th line
     *     System.out.println("Line 6: " + line);
     * } catch (UncheckedIOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param lineIndex the index of the line to read, starting from 0 for the first line, must be &gt;= 0.
     * @return a {@code String} containing the specified line, or {@code null} if the file has fewer lines.
     * @throws IllegalArgumentException if {@code lineIndex} is negative, or if {@code source} is {@code null} or is a directory rather than a
     *         file.
     * @throws UncheckedIOException if opening or reading {@code source} or closing its internally opened input fails
     */
    @MayReturnNull
    public static String readLine(final File source, final Charset charset, final int lineIndex) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(lineIndex, cs.lineIndex);

        try {
            // Buffered: see readFirstLine(File, Charset).
            return withOpenedFile(source, is -> withPooledBufferedReader(createReader(is, charset), br -> readLine(br, lineIndex)));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads a specific line from a {@code Reader} and returns it as a {@code String}.
     * The line to read is determined by the provided line index (0-based). The input reader is not closed by this method.
     *
     * <p><b>Reader position:</b> the reader is left immediately after the last line returned, so the caller can
     * carry on reading from it. Exactness costs a character-at-a-time read, plus one character of look-ahead to
     * tell a lone {@code '\r'} from the first half of {@code "\r\n"}; a reader supporting
     * {@link Reader#mark(int)} is put back exactly (the look-ahead sets a mark of its own, so a mark the caller
     * had set before the call no longer holds), and for one that does not, only a lone {@code '\r'} ending
     * the last line read keeps a character the caller would otherwise still see. A source that is already a
     * {@link java.io.BufferedReader} is read through directly, at full speed and exactly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("text_file.txt")) {
     *     String line = IOUtil.readLine(reader, 4);   // Read the 5th line
     *     System.out.println("Line 5: " + line);
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read the line from, must not be {@code null}.
     * @param lineIndex the index of the line to read, starting from 0 for the first line.
     * @return a {@code String} containing the specified line from the reader, or {@code null} if the reader has fewer lines.
     * @throws IllegalArgumentException if {@code source} is {@code null}, or if the line index is negative.
     * @throws UncheckedIOException if reading from {@code source} fails
     */
    @MayReturnNull
    @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
    public static String readLine(final Reader source, int lineIndex) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNegative(lineIndex, cs.lineIndex);

        // Reads exactly as far as the line it returns; see ExactLineReader and readLines(Reader, int, int).
        final ExactLineReader lineReader = new ExactLineReader(source);

        try {
            while (lineIndex-- > 0 && lineReader.readLine() != null) { // NOSONAR
                // continue
            }

            // Intentionally returns null (rather than throwing) when lineIndex exceeds the total
            // line count of the reader/file; see the @return doc above and @MayReturnNull.
            return lineReader.readLine();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lineReader.recycle();
        }
    }

    /**
     * Reads data from a file into a byte array buffer.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read from, must not be {@code null}.
     * @param buf the byte array buffer where the data is to be stored, must not be {@code null}.
     * @return the total number of bytes read into the buffer, or {@code -1} if there is no more data because the end of the file has been reached.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}, or {@code source} is a directory rather than a file.
     * @throws IOException if opening or reading {@code source} or closing its internally opened input fails
     */
    public static int read(final File source, final byte[] buf) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a byte array buffer with specified offset and length.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * byte[] buffer = new byte[1024];
     * try {
     *     int bytesRead = IOUtil.read(file, buffer, 10, 100);   // Read 100 bytes into buffer starting at index 10
     *     System.out.println("Read " + bytesRead + " bytes");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read data from, must not be {@code null}.
     * @param buf the byte array buffer where the data is to be stored, must not be {@code null}.
     * @param off the start offset in the array at which the data is written.
     * @param len the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of
     *         the file has been reached. A {@code len} of 0 returns 0 without reading anything, so it never
     *         reports end-of-file - matching {@link #read(InputStream, byte[], int, int)}.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}, or is a directory rather than a file.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is out of bounds for {@code buf}.
     * @throws IOException if opening or reading {@code source} or closing its internally opened input fails
     */
    public static int read(final File source, final byte[] buf, final int off, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

        // The source is validated first, as the InputStream twin does, so a call that is wrong twice over reports
        // the same argument on both paths. The range is then checked before the file is opened rather than from
        // inside the lambda, so a bad range does not cost a file handle - and so the two File-based read(..) forms
        // fail identically whether or not the file exists.
        checkBufferRange(off, len, buf.length);

        return withOpenedFile(source, is -> read(is, buf, off, len));
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
     * @param source the InputStream to read data from, must not be {@code null}.
     * @param buf the byte array buffer where the data is to be stored, must not be {@code null}.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}.
     * @throws IOException if reading from {@code source} fails
     */
    public static int read(final InputStream source, final byte[] buf) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

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
     *     int bytesRead = IOUtil.read(inputStream, buffer, 50, 200);   // Read 200 bytes into buffer starting at index 50
     *     System.out.println("Read " + bytesRead + " bytes");
     * } catch (IOException e) {
     *     System.err.println("Error reading stream: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the InputStream to read data from, must not be {@code null}.
     * @param buf the byte array buffer where the data is to be stored, must not be {@code null}.
     * @param off the start offset in the array at which the data is written.
     * @param len the maximum number of bytes to read. Unlike {@link InputStream#read(byte[], int, int)}, this method
     *            loops until {@code len} bytes are filled, EOF occurs, or a read returns zero.
     * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the
     *         stream has been reached. A {@code len} of 0 returns 0 without touching the stream, so it never reports
     *         end-of-stream.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is out of bounds for {@code buf}.
     * @throws IOException if reading from {@code source} fails
     */
    public static int read(final InputStream source, final byte[] buf, final int off, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

        checkBufferRange(off, len, buf.length);

        if (len == 0) {
            return 0;
        }

        int n = source.read(buf, off, len);

        if (n < 0 || n == len) {
            return n;
        }

        while (n < len) {
            final int n1 = source.read(buf, off + n, len - n);

            if (n1 <= 0) {
                // n1 < 0: end of stream. n1 == 0: source made no progress; treat as EOF for this call
                // rather than spinning forever (e.g., non-blocking streams, exhausted channels).
                break;
            }

            n += n1;
        }

        return n;
    }

    /**
     * Reads data from a file into a char array buffer using the default charset.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read data from, must not be {@code null}.
     * @param buf the char array buffer where the data is to be stored, must not be {@code null}.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}, or is a directory rather than a file.
     * @throws IOException if opening or reading {@code source} or closing its internally opened input fails
     */
    public static int read(final File source, final char[] buf) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

        return read(source, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a char array buffer using the provided charset.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
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
     * @param source the file to read data from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param buf the char array buffer where the data is to be stored, must not be {@code null}.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the file has been reached.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}, or is a directory rather than a file.
     * @throws IOException if opening or reading {@code source} or closing its internally opened input fails
     */
    public static int read(final File source, final Charset charset, final char[] buf) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

        return read(source, charset, buf, 0, buf.length);
    }

    /**
     * Reads data from a file into a char array buffer using the default charset with specified offset and length.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * char[] buffer = new char[1024];
     * try {
     *     int charsRead = IOUtil.read(file, buffer, 10, 100);   // Read 100 chars into buffer starting at index 10
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read data from, must not be {@code null}.
     * @param buf the char array buffer where the data is to be stored, must not be {@code null}.
     * @param off the start offset in the array at which the data is written.
     * @param len the maximum number of chars to read.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of
     *         the file has been reached. A {@code len} of 0 returns 0 without reading anything, so it never
     *         reports end-of-file - matching {@link #read(Reader, char[], int, int)}.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}, or is a directory rather than a file.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is out of bounds for {@code buf}.
     * @throws IOException if opening or reading {@code source} or closing its internally opened input fails
     */
    public static int read(final File source, final char[] buf, final int off, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

        return read(source, DEFAULT_CHARSET, buf, off, len);
    }

    /**
     * Reads data from a file into a char array buffer using the provided charset with specified offset and length.
     * This method handles regular files, gzipped files (.gz), and zip files (.zip, reading the first non-directory entry).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text_file.txt");
     * char[] buffer = new char[1024];
     * try {
     *     int charsRead = IOUtil.read(file, StandardCharsets.UTF_8, buffer, 20, 150);   // Read 150 chars starting at index 20
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading file: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the file to read data from, must not be {@code null}.
     * @param charset the character set to use for decoding, if {@code null} the default charset (UTF-8) is used.
     * @param buf the char array buffer where the data is to be stored, must not be {@code null}.
     * @param off the start offset in the array at which the data is written.
     * @param len the maximum number of chars to read.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of
     *         the file has been reached. A {@code len} of 0 returns 0 without reading anything, so it never
     *         reports end-of-file - matching {@link #read(Reader, char[], int, int)}.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}, or is a directory rather than a file.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is out of bounds for {@code buf}.
     * @throws IOException if opening or reading {@code source} or closing its internally opened input fails
     */
    public static int read(final File source, final Charset charset, final char[] buf, final int off, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

        // See read(File, byte[], int, int): validate the source, then the range, before opening the file.
        checkBufferRange(off, len, buf.length);

        return withOpenedFile(source, is -> read(createReader(is, charset), buf, off, len));
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
     * @param source the Reader to read data from, must not be {@code null}.
     * @param buf the char array buffer where the data is to be stored, must not be {@code null}.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}.
     * @throws IOException if reading from {@code source} fails
     */
    public static int read(final Reader source, final char[] buf) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

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
     *     int charsRead = IOUtil.read(reader, buffer, 50, 200);   // Read 200 chars into buffer starting at index 50
     *     System.out.println("Read " + charsRead + " characters");
     * } catch (IOException e) {
     *     System.err.println("Error reading from reader: " + e.getMessage());
     * }
     * }</pre>
     *
     * @param source the Reader to read data from, must not be {@code null}.
     * @param buf the char array buffer where the data is to be stored, must not be {@code null}.
     * @param off the start offset in the array at which the data is written.
     * @param len the maximum number of chars to read. Unlike {@link Reader#read(char[], int, int)}, this method
     *            loops until {@code len} chars are filled, EOF occurs, or a read returns zero.
     * @return the total number of chars read into the buffer, or -1 if there is no more data because the end of the
     *         stream has been reached. A {@code len} of 0 returns 0 without touching the reader, so it never reports
     *         end-of-stream.
     * @throws IllegalArgumentException if {@code source} or {@code buf} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code off} or {@code len} is out of bounds for {@code buf}.
     * @throws IOException if reading from {@code source} fails
     */
    public static int read(final Reader source, final char[] buf, final int off, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(buf, cs.buf);

        checkBufferRange(off, len, buf.length);

        if (len == 0) {
            return 0;
        }

        int n = source.read(buf, off, len);

        if (n < 0 || n == len) {
            return n;
        }

        while (n < len) {
            final int n1 = source.read(buf, off + n, len - n);

            if (n1 <= 0) {
                // n1 < 0: end of stream. n1 == 0: source made no progress; treat as EOF for this call
                // rather than spinning forever.
                break;
            }

            n += n1;
        }

        return n;
    }

    /**
     * Validates an {@code (off, len)} pair against a buffer length for the low-level {@code read(..)} overloads.
     *
     * <p>Reports {@link IndexOutOfBoundsException} for a negative {@code len} as well as for an out-of-range
     * one, matching {@link InputStream#read(byte[], int, int)} and this family's own documented contract -
     * which is why {@code N.checkFromIndexSize(..)}, whose negative-size case is an
     * {@code IllegalArgumentException}, is not used here.
     *
     * @param off the start offset in the buffer.
     * @param len the number of bytes/chars wanted.
     * @param bufLength the length of the buffer.
     * @throws IndexOutOfBoundsException if the range does not fit the buffer.
     */
    private static void checkBufferRange(final int off, final int len, final int bufLength) throws IndexOutOfBoundsException {
        if ((off < 0) || (off > bufLength) || (len < 0) || ((off + len) > bufLength) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException("Offset " + off + " with length " + len + " is out-of-bounds for a buffer of length " + bufLength);
        }
    }

    static InputStreamReader createReader(final InputStream source, final Charset encoding) {
        return IOUtil.newInputStreamReader(source, encoding); // newInputStreamReader already maps a null charset to UTF-8
    }

    /**
     * Wraps {@code reader} in a pooled buffer, applies {@code action} to it, and returns the buffer to the pool.
     *
     * <p>For a reader this class has just opened and will discard. The line-slicing {@code Reader} overloads
     * read one character at a time so that a <i>caller-owned</i> reader is never read past the line it returns
     * (see {@link ExactLineReader}); that costs real throughput and buys nothing for a reader nobody outside
     * this call can observe, so the {@code File} and {@code InputStream} entry points hand in a buffer instead
     * and take the fast path.
     *
     * @param <R>    the result type.
     * @param <E>    the exception {@code action} may throw.
     * @param reader the freshly opened reader to buffer.
     * @param action what to do with the buffered reader.
     * @return whatever {@code action} returns.
     * @throws E if {@code action} throws.
     */
    private static <R, E extends Exception> R withPooledBufferedReader(final Reader reader,
            final Throwables.Function<? super java.io.BufferedReader, ? extends R, E> action) throws E {
        final java.io.BufferedReader br = Objectory.createBufferedReader(reader);

        try {
            return action.apply(br);
        } finally {
            Objectory.recycle(br);
        }
    }

    /**
     * Reads lines from a caller-owned {@link Reader} without reading past the line it hands back.
     *
     * <p>The line-slicing overloads used to wrap a non-{@code BufferedReader} source in a pooled buffer and
     * then throw that buffer away, so {@code readFirstLine(reader)} returned line 1 and left the caller holding
     * an exhausted reader: everything the buffer had pulled in beyond the returned line was simply gone, with
     * no exception and no short read to show for it. This reads one character at a time instead, which is what
     * "consume no more than you return" costs. A source that is already a {@link java.io.BufferedReader} is the
     * caller's own buffer and can be read through at full speed, so it is; and the {@code File} and
     * {@code InputStream} entry points hand in a buffer of their own (see
     * {@link #withPooledBufferedReader(Reader, Throwables.Function)}), so they never pay the character-at-a-time
     * cost either.
     *
     * <p>Line terminators are {@link java.io.BufferedReader#readLine()}'s: {@code '\n'}, {@code '\r'} or
     * {@code "\r\n"}. Telling a lone {@code '\r'} apart from the first half of {@code "\r\n"} needs one
     * character of look-ahead, which is the one place exactness is not free. A reader that supports
     * {@link Reader#mark(int)} - {@link StringReader} and {@link java.io.CharArrayReader} among them - is put
     * back exactly. For one that does not, the character looked at is held here and starts the next line of the
     * <i>same</i> call, so only a lone {@code '\r'} ending the last line a call reads keeps a character the
     * caller would otherwise still see.
     */
    private static final class ExactLineReader {

        /** Non-{@code null} when the caller's reader is already a buffer of their own, which may be read through. */
        private final BufferedReader buffered;

        private final Reader source;

        private final boolean markSupported;

        /** Allocated on the first line and reused for every later one; released by {@link #recycle()}. */
        private StringBuilder sb;

        /** A character read past a {@code '\r'} that turned out not to be {@code '\n'}. */
        private int carry;

        private boolean hasCarry;

        ExactLineReader(final Reader source) {
            this.source = source;
            buffered = isBufferedReader(source) ? (BufferedReader) source : null;
            markSupported = buffered == null && source.markSupported();
        }

        /**
         * Reads the next line.
         *
         * @return the line without its terminator, or {@code null} at end of input.
         * @throws IOException if reading from the underlying reader fails
         */
        @MayReturnNull
        String readLine() throws IOException {
            if (buffered != null) {
                return buffered.readLine();
            }

            int c = read();

            if (c == EOF) {
                return null;
            }

            if (sb == null) {
                sb = Objectory.createStringBuilder();
            } else {
                sb.setLength(0);
            }

            while (c != EOF && c != '\n' && c != '\r') {
                sb.append((char) c);
                c = read();
            }

            if (c == '\r') {
                skipLineFeed();
            }

            return sb.toString();
        }

        /** Returns the pooled builder. Call once, when no more lines are wanted. */
        void recycle() {
            Objectory.recycle(sb);
            sb = null;
        }

        private int read() throws IOException {
            if (hasCarry) {
                hasCarry = false;

                return carry;
            }

            return source.read();
        }

        /**
         * Consumes the {@code '\n'} of a {@code "\r\n"} pair, and only that: anything else stays available to
         * the caller, either by being put back or by being carried into the next line of this call.
         */
        private void skipLineFeed() throws IOException {
            if (markSupported) {
                source.mark(1);

                if (source.read() != '\n') {
                    source.reset();
                }

                return;
            }

            final int c = source.read();

            if (c != '\n' && c != EOF) {
                carry = c;
                hasCarry = true;
            }
        }
    }

    /**
     * Writes the string representation of an Object to a file as a single line using the default charset.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     * The line is always terminated with the Unix line separator ({@code "\n"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File outputFile = new File("output.txt");
     * IOUtil.writeLine("Hello, World!", outputFile);   // Writes "Hello, World!\n" (overwrites)
     * }</pre>
     *
     * @param obj the Object to be written.
     * @param output the file where the object's string representation is to be written, must not be {@code null}.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #writeLine(Object, Charset, File)
     * @see #appendLine(Object, File)
     * @see N#toString(Object)
     */
    public static void writeLine(final Object obj, final File output) throws IllegalArgumentException, IOException {
        writeLine(obj, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the string representation of an Object to a file as a single line using the specified Charset.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     * The line is always terminated with the Unix line separator ({@code "\n"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File outputFile = new File("output.txt");
     * IOUtil.writeLine("Hello, World!", StandardCharsets.UTF_8, outputFile);   // Writes "Hello, World!\n" (overwrites)
     * }</pre>
     *
     * @param obj the Object to be written; {@code null} is written as the four-character text {@code "null"}.
     * @param charset the Charset used to encode the line, if {@code null} the default charset (UTF-8) is used.
     * @param output the file where the object's string representation is to be written, must not be {@code null}.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #appendLine(Object, Charset, File)
     * @see N#toString(Object)
     */
    public static void writeLine(final Object obj, final Charset charset, final File output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        // Render before opening: opening truncates, and a toString() that throws would otherwise leave an
        // existing file empty. appendLine(Object, Charset, File) already works this way.
        final String line = N.toString(obj) + IOUtil.LINE_SEPARATOR_UNIX;

        write(toByteArray(line, charset), output);
    }

    /**
     * Writes the string representation of an Object to a Writer as a single line.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     * The line is always terminated with the Unix line separator ({@code "\n"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.writeLine("First line", writer);
     *     IOUtil.writeLine(123, writer);
     * }
     * }</pre>
     *
     * @param obj the Object to be written.
     * @param output the Writer where the object's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see N#toString(Object)
     */
    public static void writeLine(final Object obj, final Writer output) throws IllegalArgumentException, IOException {
        writeLine(obj, output, false);
    }

    /**
     * Writes the string representation of an Object to a Writer as a single line.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     * The line is always terminated with the Unix line separator ({@code "\n"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.writeLine("Hello", writer, true);  // Writes and flushes immediately
     *     IOUtil.writeLine("World", writer, false); // Writes without flushing
     * }
     * }</pre>
     *
     * @param obj the Object to be written.
     * @param output the Writer where the object's string representation is to be written, must not be {@code null}.
     * @param flush if {@code true}, the stream will be flushed after writing the line.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see N#toString(Object)
     */
    public static void writeLine(final Object obj, final Writer output, final boolean flush) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

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
     * Each line is terminated with the Unix line separator ({@code "\n"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> data = Arrays.asList("Line 1", "Line 2", "Line 3");
     * File output = new File("output.txt");
     * IOUtil.writeLines(data.iterator(), output);
     * }</pre>
     *
     * @param lines the Iterator containing the objects to be written; {@code null} or exhausted is treated as empty.
     * @param output the File where the objects' string representations are to be written, must not be {@code null}.
     *      It is created, along with any missing parent directories, if it does not exist; an existing file is always
     *      truncated first, so writing no lines leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #writeLines(Iterator, Charset, File)
     * @see #appendLines(Iterator, File)
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterator<?> lines, final File output) throws IllegalArgumentException, IOException {
        writeLines(lines, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the string representation of each object in an Iterator to a file using the specified Charset. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     * Each line is terminated with the Unix line separator ({@code "\n"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> data = Arrays.asList("Line 1", "Line 2", "Line 3");
     * File output = new File("output.txt");
     * IOUtil.writeLines(data.iterator(), StandardCharsets.UTF_8, output);
     * }</pre>
     *
     * @param lines the Iterator containing the objects to be written; {@code null} or exhausted is treated as empty.
     * @param charset the Charset used to encode the lines, if {@code null} the default charset (UTF-8) is used.
     * @param output the File where the objects' string representations are to be written, must not be {@code null}.
     *      It is created, along with any missing parent directories, if it does not exist; an existing file is always
     *      truncated first, so writing no lines leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #appendLines(Iterator, Charset, File)
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterator<?> lines, final Charset charset, final File output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        try (Writer writer = openFileWriter(output, checkCharset(charset))) {
            writeLines(lines, writer, true);
        }
    }

    /**
     * Writes the string representation of each object in an Iterator to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
     * try (Writer writer = new FileWriter("numbers.txt")) {
     *     IOUtil.writeLines(numbers.iterator(), writer);
     * }
     * }</pre>
     *
     * @param lines the Iterator containing the objects to be written; {@code null} or exhausted is treated as empty.
     * @param output the Writer where the objects' string representations are to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterator<?> lines, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     * Writes the string representation of each object in an Iterator to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> logs = Arrays.asList("INFO: Start", "DEBUG: Processing", "INFO: Done");
     * try (Writer writer = new FileWriter("log.txt")) {
     *     IOUtil.writeLines(logs.iterator(), writer, true);  // Flush after writing
     * }
     * }</pre>
     *
     * @param lines the Iterator containing the objects to be written; {@code null} or exhausted is treated as empty.
     * @param output the Writer where the objects' string representations are to be written, must not be {@code null}.
     * @param flush if {@code true}, {@code output} is flushed after the lines are written; if {@code false} it is
     *        not flushed at all, whatever kind of {@code Writer} it is. (Internally a non-buffered
     *        {@code Writer} is wrapped in a pooled buffer, which is handed over to {@code output} either way -
     *        so every line always reaches {@code output}, flushed or not.)
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterator<?> lines, final Writer output, final boolean flush) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(lines)) {
            if (flush) {
                output.flush();
            }

            return;
        }

        final boolean isBufferedWriter = isBufferedWriter(output);
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR

        try {
            while (true) {
                final String text;

                // Only the iteration (hasNext/next and the element's toString) is guarded: when IT fails, the lines
                // already accepted are sitting in the pooled buffer and releasing it would discard them, so they are
                // handed over first, as the documentation promises. A failure of the destination itself - an
                // IOException, or an unchecked exception from a custom Writer - must not take that route: re-driving
                // the buffer would resend what the destination had already consumed.
                try {
                    if (!lines.hasNext()) {
                        break;
                    }

                    text = nextLineText(lines);
                } catch (final RuntimeException | Error e) {
                    handOverPooledLines(bw, isBufferedWriter, e);
                    throw e;
                }

                writeLineText(bw, text);
            }

            if (flush) {
                bw.flush();
            } else if (!isBufferedWriter) {
                drainPooledWriter(bw);
            }
        } finally {
            if (!isBufferedWriter) {
                releasePooledWriter((BufferedWriter) bw);
            }
        }
    }

    /**
     * The text {@code writeLines} writes for the iterator's next element: {@code "null"} for a {@code null} element,
     * its {@code toString()} otherwise.
     */
    private static String nextLineText(final Iterator<?> lines) {
        final Object line = lines.next();

        return line == null ? null : N.toString(line);
    }

    /**
     * Writes one line of {@code writeLines} output: the text ({@code "null"} when {@code null}) and a {@code '\n'}.
     */
    private static void writeLineText(final Writer bw, final String text) throws IOException {
        if (text == null) {
            bw.write(Strings.NULL_CHAR_ARRAY);
        } else {
            bw.write(text);
        }

        bw.write(IOUtil.LINE_SEPARATOR_UNIX);
    }

    /**
     * Writes the string representation of each object in an Iterable to a file. Each object is written as a single line using the default charset.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     * Each line is terminated with the Unix line separator ({@code "\n"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> lines = Arrays.asList("First line", "Second line", "Third line");
     * File output = new File("output.txt");
     * IOUtil.writeLines(lines, output);
     * }</pre>
     *
     * @param lines the Iterable containing the objects to be written; {@code null} or empty is treated as empty.
     * @param output the File where the objects' string representations are to be written, must not be {@code null}.
     *      It is created, along with any missing parent directories, if it does not exist; an existing file is always
     *      truncated first, so writing no lines leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #writeLines(Iterable, Charset, File)
     * @see #appendLines(Iterable, File)
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterable<?> lines, final File output) throws IllegalArgumentException, IOException {
        writeLines(lines, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the string representation of each object in an Iterable to a file using the specified Charset. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     * Each line is terminated with the Unix line separator ({@code "\n"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> lines = Arrays.asList("First line", "Second line", "Third line");
     * File output = new File("output.txt");
     * IOUtil.writeLines(lines, StandardCharsets.UTF_8, output);
     * }</pre>
     *
     * @param lines the Iterable containing the objects to be written; {@code null} or empty is treated as empty.
     * @param charset the Charset used to encode the lines, if {@code null} the default charset (UTF-8) is used.
     * @param output the File where the objects' string representations are to be written, must not be {@code null}.
     *      It is created, along with any missing parent directories, if it does not exist; an existing file is always
     *      truncated first, so writing no lines leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #appendLines(Iterable, Charset, File)
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterable<?> lines, final Charset charset, final File output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        try (Writer writer = openFileWriter(output, checkCharset(charset))) {
            writeLines(lines, writer, true);
        }
    }

    /**
     * Writes the string representation of each object in an Iterable to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> uniqueNames = N.asSet("Alice", "Bob", "Charlie");
     * try (Writer writer = new FileWriter("names.txt")) {
     *     IOUtil.writeLines(uniqueNames, writer);
     * }
     * }</pre>
     *
     * @param lines the Iterable containing the objects to be written; {@code null} or empty is treated as empty.
     * @param output the Writer where the objects' string representations are to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterable<?> lines, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmptyCollection(lines)) {
            return;
        }

        writeLines(lines, output, false);
    }

    /**
     * Writes the string representation of each object in an Iterable to a Writer. Each object is written as a single line.
     * The string representation of each object is obtained by calling {@code N.toString(obj)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> numbers = N.asList(10, 20, 30, 40);
     * try (Writer writer = new FileWriter("numbers.txt")) {
     *     IOUtil.writeLines(numbers, writer, true);  // Write and flush
     * }
     * }</pre>
     *
     * @param lines the Iterable containing the objects to be written; {@code null} or empty is treated as empty.
     * @param output the Writer where the objects' string representations are to be written, must not be {@code null}.
     * @param flush if {@code true}, {@code output} is flushed after the lines are written; if {@code false} it is
     *        not flushed at all, whatever kind of {@code Writer} it is. (Internally a non-buffered
     *        {@code Writer} is wrapped in a pooled buffer, which is handed over to {@code output} either way -
     *        so every line always reaches {@code output}, flushed or not.)
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see N#toString(Object)
     */
    public static void writeLines(final Iterable<?> lines, final Writer output, final boolean flush) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmptyCollection(lines)) {
            if (flush) {
                output.flush();
            }

            return;
        }

        final boolean isBufferedWriter = isBufferedWriter(output);
        final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR

        try {
            final Iterator<?> iter;

            try {
                iter = lines.iterator();
            } catch (final RuntimeException | Error e) {
                handOverPooledLines(bw, isBufferedWriter, e);
                throw e;
            }

            while (true) {
                final String text;

                // See writeLines(Iterator, Writer, boolean): only the iteration is guarded, so an iteration failure
                // hands over the accepted lines while a destination failure never re-drives the buffer.
                try {
                    if (!iter.hasNext()) {
                        break;
                    }

                    text = nextLineText(iter);
                } catch (final RuntimeException | Error e) {
                    handOverPooledLines(bw, isBufferedWriter, e);
                    throw e;
                }

                writeLineText(bw, text);
            }

            if (flush) {
                bw.flush();
            } else if (!isBufferedWriter) {
                drainPooledWriter(bw);
            }
        } finally {
            if (!isBufferedWriter) {
                releasePooledWriter((BufferedWriter) bw);
            }
        }
    }

    /**
     * Drains the pooled writer used by {@code writeLines} into the caller's {@code Writer} after the iteration
     * (not the destination) failed, so that every line accepted before the failure still reaches the output; a
     * drain failure is attached to the primary failure as a suppressed exception.
     *
     * @param bw               the writer the lines were written to.
     * @param isBufferedWriter whether {@code bw} is the caller's own buffer (nothing to drain).
     * @param primary          the failure being propagated.
     */
    private static void handOverPooledLines(final Writer bw, final boolean isBufferedWriter, final Throwable primary) {
        if (isBufferedWriter) {
            return;
        }

        try {
            drainPooledWriter(bw);
        } catch (final Throwable suppressed) { // NOSONAR - the primary failure is what propagates; an Error here rides along
            // A source and writer may reuse the same failure; self-suppression would replace it with an IllegalArgumentException.
            if (primary != suppressed) {
                primary.addSuppressed(suppressed);
            }
        }
    }

    /**
     * Hands the characters a pooled {@link BufferedWriter} still holds to the caller's {@code Writer} without
     * flushing that writer.
     *
     * <p>The wrapper has to be drained or its buffered characters are lost when it is recycled - but draining
     * is not flushing. {@link java.io.BufferedWriter#flush()} also flushes its destination, so
     * {@code writeLines(.., writer, false)} used to flush the caller's {@code Writer} anyway whenever that
     * writer was not already a {@code java.io.BufferedWriter} and therefore had to be wrapped: {@code flush}
     * meant "do not flush" for one kind of {@code Writer} and nothing at all for the other.
     * {@code flushBufferToWriter()} moves the characters on and leaves the destination alone.
     *
     * @param bw the pooled writer to drain.
     * @throws IOException if writing the buffered characters fails.
     */
    private static void drainPooledWriter(final Writer bw) throws IOException {
        // Fully qualified: this file imports java.io.BufferedWriter, so the simple name is the JDK type.
        if (bw instanceof final com.landawn.abacus.util.BufferedWriter pooled) {
            pooled.flushBufferToWriter();
        } else {
            bw.flush();
        }
    }

    /**
     * Releases the pooled {@link BufferedWriter} taken for one {@code writeLines} call back to the pool.
     *
     * <p>{@link Objectory#recycle(java.io.BufferedWriter)} flushes whatever the writer still holds and reports
     * a failure as an unchecked {@code UncheckedIOException}. That flush must never be the one that reaches
     * the destination, for two separate reasons, so the writer is always cleared with {@code _reset()} first -
     * which drops both the buffer and the destination reference and so makes the flush inside
     * {@code recycle(..)} a no-op, while the writer still goes back to the pool.
     *
     * <p><b>After a failure</b> the destination has just failed, so flushing into it fails again - and thrown
     * from a {@code finally} that exception <i>replaced</i> the primary one and escaped a method that declares
     * {@code IOException}, as an unchecked type no {@code catch (IOException)} ever sees. Which of the two a
     * caller got depended on whether their {@code Writer} happened to be a {@code java.io.BufferedWriter}
     * already, in which case no pooled writer is taken at all.
     *
     * <p><b>After a success</b> the buffer has already been dealt with by the caller - flushed when
     * {@code flush} was requested, otherwise merely drained by {@link #drainPooledWriter(Writer)} - so this
     * flush has nothing left to write, and letting it run would undo {@code drainPooledWriter}'s whole point
     * by flushing the caller's writer after all.
     *
     * @param bw the pooled writer to release.
     */
    private static void releasePooledWriter(final BufferedWriter bw) {
        // Fully qualified: this file imports java.io.BufferedWriter, so the simple name is the JDK type.
        if (bw instanceof final com.landawn.abacus.util.BufferedWriter pooled) {
            pooled._reset();
        }

        Objectory.recycle(bw);
    }

    /**
     * Writes the string representation of a boolean to a Writer.
     *
     * <p><b>Family rule:</b> every primitive {@code write(.., Writer)} overload writes the value's natural <i>text</i>
     * form. For the numeric types that is the decimal representation, and for {@code char} it is the character itself,
     * so {@code write((byte) 65, w)} emits {@code 65} while {@code write((char) 65, w)} emits {@code A}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(true, writer);
     *     IOUtil.write(false, writer);
     * }
     * }</pre>
     *
     * @param value the boolean value to be written; {@code true} emits {@code true} and {@code false} emits {@code false}.
     * @param output the Writer where the boolean's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(char, Writer)
     */
    public static void write(final boolean value, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.write(N.stringOf(value));
    }

    /**
     * Writes a single character to a Writer.
     *
     * <p><b>Family rule:</b> every primitive {@code write(.., Writer)} overload writes the value's natural <i>text</i>
     * form. A {@code char} is text, so it is written as the character itself - unlike the numeric overloads, which
     * write a decimal representation. {@code write((char) 65, w)} therefore emits {@code A} while
     * {@code write((byte) 65, w)} emits {@code 65}. This overload is equivalent to {@link Writer#write(int)}.
     *
     * <p><b>Note:</b> do not let this overload disappear from a call site. {@code char} widens to {@code int}, so if
     * it were removed or renamed, {@code IOUtil.write('A', writer)} would still compile - silently binding to
     * {@link #write(int, Writer)} and writing {@code 65} instead of {@code A}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write('A', writer);          // writes the one character A, not "65"
     *     IOUtil.write('\n', writer);
     * }
     * }</pre>
     *
     * @param value the character to be written; written as itself, not as its numeric code point.
     * @param output the Writer where the character is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(int, Writer)
     * @see Writer#write(int)
     */
    public static void write(final char value, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.write(value);
    }

    /**
     * Writes the string representation of a byte to a Writer.
     *
     * <p><b>Family rule:</b> every primitive {@code write(.., Writer)} overload writes the value's natural <i>text</i>
     * form. For the numeric types that is the decimal representation, and for {@code char} it is the character itself,
     * so {@code write((byte) 65, w)} emits {@code 65} while {@code write((char) 65, w)} emits {@code A}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write((byte) 65, writer);  // Writes "65"
     * }
     * }</pre>
     *
     * @param value the byte value to be written; written in decimal, so {@code (byte) 65} emits {@code 65}, not {@code A}.
     * @param output the Writer where the byte's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(char, Writer)
     */
    public static void write(final byte value, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.write(N.stringOf(value));
    }

    /**
     * Writes the string representation of a short to a Writer.
     *
     * <p><b>Family rule:</b> every primitive {@code write(.., Writer)} overload writes the value's natural <i>text</i>
     * form. For the numeric types that is the decimal representation, and for {@code char} it is the character itself,
     * so {@code write((byte) 65, w)} emits {@code 65} while {@code write((char) 65, w)} emits {@code A}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write((short) 100, writer);  // Writes "100"
     * }
     * }</pre>
     *
     * @param value the short value to be written; written in decimal.
     * @param output the Writer where the short's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(char, Writer)
     */
    public static void write(final short value, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.write(N.stringOf(value));
    }

    /**
     * Writes the string representation of an integer to a Writer.
     *
     * <p><b>Family rule:</b> every primitive {@code write(.., Writer)} overload writes the value's natural <i>text</i>
     * form. For the numeric types that is the decimal representation, and for {@code char} it is the character itself,
     * so {@code write((byte) 65, w)} emits {@code 65} while {@code write((char) 65, w)} emits {@code A}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(12345, writer);  // Writes "12345"
     * }
     * }</pre>
     *
     * @param value the integer value to be written; written in decimal. To emit a code point as a character, pass a
     *              {@code char} so that {@link #write(char, Writer)} is selected instead.
     * @param output the Writer where the integer's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(char, Writer)
     */
    public static void write(final int value, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.write(N.stringOf(value));
    }

    /**
     * Writes the string representation of a long to a Writer.
     *
     * <p><b>Family rule:</b> every primitive {@code write(.., Writer)} overload writes the value's natural <i>text</i>
     * form. For the numeric types that is the decimal representation, and for {@code char} it is the character itself,
     * so {@code write((byte) 65, w)} emits {@code 65} while {@code write((char) 65, w)} emits {@code A}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(123456789L, writer);  // Writes "123456789"
     * }
     * }</pre>
     *
     * @param lng the long value to be written; written in decimal.
     * @param output the Writer where the long's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(char, Writer)
     */
    public static void write(final long lng, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.write(N.stringOf(lng));
    }

    /**
     * Writes the string representation of a float to a Writer.
     *
     * <p><b>Family rule:</b> every primitive {@code write(.., Writer)} overload writes the value's natural <i>text</i>
     * form. For the numeric types that is the decimal representation, and for {@code char} it is the character itself,
     * so {@code write((byte) 65, w)} emits {@code 65} while {@code write((char) 65, w)} emits {@code A}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(3.14f, writer);  // Writes "3.14"
     * }
     * }</pre>
     *
     * @param value the float value to be written; written as {@link Float#toString(float)} would render it, so
     *              {@code NaN}, {@code Infinity} and {@code -0.0} appear in that textual form.
     * @param output the Writer where the float's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(char, Writer)
     */
    public static void write(final float value, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.write(N.stringOf(value));
    }

    /**
     * Writes the string representation of a double to a Writer.
     *
     * <p><b>Family rule:</b> every primitive {@code write(.., Writer)} overload writes the value's natural <i>text</i>
     * form. For the numeric types that is the decimal representation, and for {@code char} it is the character itself,
     * so {@code write((byte) 65, w)} emits {@code 65} while {@code write((char) 65, w)} emits {@code A}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(3.14159, writer);  // Writes "3.14159"
     * }
     * }</pre>
     *
     * @param value the double value to be written; written as {@link Double#toString(double)} would render it, so
     *              {@code NaN}, {@code Infinity} and {@code -0.0} appear in that textual form.
     * @param output the Writer where the double's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(char, Writer)
     */
    public static void write(final double value, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.write(N.stringOf(value));
    }

    /**
     * Writes the string representation of an object to a Writer.
     * The string representation of the object is obtained by calling {@code N.toString(obj)}.
     *
     * <p><b>Family rule:</b> this is the fallback of the {@code write(.., Writer)} family, which writes each value's
     * natural <i>text</i> form (see {@link #write(char, Writer)}). A boxed value reaches this overload rather than the
     * matching primitive one, so {@code write(Character.valueOf('A'), w)} emits {@code A} by way of {@code toString()} -
     * the same output the {@code char} overload gives - while {@code write(Byte.valueOf((byte) 65), w)} emits
     * {@code 65}. It also accepts {@code null}, which the primitive overloads cannot.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write("Hello", writer);
     *     IOUtil.write(Arrays.asList(1, 2, 3), writer);
     *     IOUtil.write((Object) null, writer);  // Writes "null"
     * }
     * }</pre>
     *
     * @param obj the object whose string representation is to be written; {@code null} is written as the
     *            four-character text {@code "null"}.
     * @param output the Writer where the object's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see N#toString(Object)
     * @see #write(char, Writer)
     */
    public static void write(final Object obj, final Writer output) throws IllegalArgumentException, IOException { // Note: DO NOT remove/update this method because it also protects write(boolean/char/byte/../double, Writer) from NullPointerException.
        N.checkArgNotNull(output, cs.output);

        output.write(N.toString(obj));
    }

    /**
     * Writes the byte array representation of a CharSequence to a File using the default charset (UTF-8).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File outputFile = new File("output.txt");
     * IOUtil.write("Hello, World!", outputFile);   // file now holds exactly "Hello, World!"
     * IOUtil.write("", outputFile);                // file now exists and is empty
     * IOUtil.write((CharSequence) null, outputFile);   // same as "": file now exists and is empty
     * }</pre>
     *
     * @param cs the CharSequence whose byte array representation is to be written; {@code null} is treated as empty.
     *           Note that the {@code OutputStream} and {@code Writer} overloads instead write the four-character text
     *           {@code "null"}, matching {@link Appendable#append(CharSequence)}; a file write replaces content rather
     *           than appending to a stream, so it treats {@code null} as "no content".
     * @param output the File where the CharSequence's byte array representation is to be written, must not be {@code null}.
     *               It is created, along with any missing parent directories, if it does not exist; an existing file is
     *               always truncated first, so writing {@code null} or {@code ""} leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see String#getBytes(Charset)
     * @see #write(CharSequence, Charset, File)
     */
    public static void write(final CharSequence cs, final File output) throws IllegalArgumentException, IOException {
        write(cs, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the byte array representation of a CharSequence to a File using the specified Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File outputFile = new File("output.txt");
     * IOUtil.write("Hello, World!", StandardCharsets.UTF_8, outputFile);
     * }</pre>
     *
     * <p><b>Memory:</b> the whole sequence is encoded in one step, so a {@code String} of the characters and a
     * {@code byte[]} of the encoded form exist alongside {@code charSequence} at the peak. For a very large
     * {@code CharSequence}, write it in slices through a {@link #newBufferedWriter(File, Charset)} instead.
     *
     * @param cs      the CharSequence whose byte array representation is to be written; {@code null} is treated as empty
     *                (unlike the {@code OutputStream}/{@code Writer} overloads, which write the text {@code "null"}).
     * @param charset the Charset to be used to encode the CharSequence into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param output  the File where the CharSequence's byte array representation is to be written, must not be {@code null}.
     *                It is created, along with any missing parent directories, if it does not exist; an existing file is
     *                always truncated first, so writing {@code null} or {@code ""} leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see String#getBytes(Charset)
     * @see #write(CharSequence, Charset, OutputStream)
     */
    public static void write(final CharSequence cs, final Charset charset, final File output) throws IllegalArgumentException, IOException {
        write(cs == null ? N.EMPTY_BYTE_ARRAY : toByteArray(cs, charset), output);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream using the default charset.
     * The output stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream output = new FileOutputStream("output.txt")) {
     *     IOUtil.write("Hello, World!", output);
     * }
     * }</pre>
     *
     * @param cs     the CharSequence whose byte array representation is to be written; {@code null} is written as the
     *               four-character text {@code "null"}, matching {@link Appendable#append(CharSequence)}.
     * @param output the OutputStream where the CharSequence's byte array representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see String#getBytes(Charset)
     * @see #write(CharSequence, Charset, OutputStream)
     */
    public static void write(final CharSequence cs, final OutputStream output) throws IllegalArgumentException, IOException {
        write(cs, output, false);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream using the specified Charset.
     * The output stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream output = new FileOutputStream("output.txt")) {
     *     IOUtil.write("Hello, World!", StandardCharsets.UTF_8, output);
     * }
     * }</pre>
     *
     * @param cs      the CharSequence whose byte array representation is to be written; {@code null} is written as the
     *                four-character text {@code "null"}, matching {@link Appendable#append(CharSequence)}. The
     *                {@code File} overloads instead treat {@code null} as "no content" - see
     *                {@link #write(CharSequence, Charset, File)}.
     * @param charset the Charset to be used to encode the CharSequence into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param output  the OutputStream where the CharSequence's byte array representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see String#getBytes(Charset)
     */
    public static void write(final CharSequence cs, final Charset charset, final OutputStream output) throws IllegalArgumentException, IOException {
        write(cs, charset, output, false);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream using the default charset.
     * The output stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream output = new FileOutputStream("output.txt")) {
     *     IOUtil.write("Hello, World!", output, true);  // Writes and flushes
     * }
     * }</pre>
     *
     * @param cs     the CharSequence whose byte array representation is to be written; {@code null} is written as the
     *               four-character text {@code "null"}, matching {@link Appendable#append(CharSequence)}.
     * @param output the OutputStream where the CharSequence's byte array representation is to be written, must not be {@code null}.
     * @param flush  if {@code true}, the output stream is flushed after writing the CharSequence.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see String#getBytes(Charset)
     * @see #write(CharSequence, Charset, OutputStream, boolean)
     */
    public static void write(final CharSequence cs, final OutputStream output, final boolean flush) throws IllegalArgumentException, IOException {
        write(cs, DEFAULT_CHARSET, output, flush);
    }

    /**
     * Writes the byte array representation of a CharSequence to an OutputStream using the specified Charset.
     * The output stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream output = new FileOutputStream("output.txt")) {
     *     IOUtil.write("Hello, World!", StandardCharsets.UTF_8, output, true);  // Writes and flushes
     * }
     * }</pre>
     *
     * <p><b>Memory:</b> the whole sequence is encoded in one step, so a {@code String} of the characters and a
     * {@code byte[]} of the encoded form exist alongside {@code cs} at the peak. For a very large
     * {@code CharSequence}, write through {@link #write(CharSequence, Writer, boolean)} (which avoids the
     * {@code byte[]} copy; {@code Writer.append} still copies a non-{@code String} sequence into a {@code String}) over an {@link OutputStreamWriter}, or write it in slices.
     *
     * @param charSequence the CharSequence whose byte array representation is to be written; {@code null} is written as the
     *                     four-character text {@code "null"}, matching {@link Appendable#append(CharSequence)}.
     * @param charset      the Charset to be used to encode the CharSequence into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param output       the OutputStream where the CharSequence's byte array representation is to be written, must not be {@code null}.
     * @param flush        if {@code true}, the output stream is flushed after writing the CharSequence.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see String#getBytes(Charset)
     * @see #write(CharSequence, Writer, boolean)
     */
    public static void write(final CharSequence charSequence, Charset charset, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        charset = checkCharset(charset);

        output.write(N.toString(charSequence).getBytes(charset));

        if (flush) {
            output.flush();
        }
    }

    /**
     * Writes the string representation of a CharSequence to a Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write("Hello, World!", writer);
     * }
     * }</pre>
     *
     * @param cs     the CharSequence whose string representation is to be written; {@code null} is written as the
     *               four-character text {@code "null"}, matching {@link Appendable#append(CharSequence)}. The
     *               {@code File} overloads instead treat {@code null} as "no content" - see
     *               {@link #write(CharSequence, File)}.
     * @param output the Writer where the CharSequence's string representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     * @see #write(CharSequence, Writer, boolean)
     * @see #write(CharSequence, File)
     */
    public static void write(final CharSequence cs, final Writer output) throws IllegalArgumentException, IOException {
        write(cs, output, false);
    }

    /**
     * Writes the string representation of a CharSequence to a Writer, optionally flushing the writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write("Hello, World!", writer, true);
     * }
     * }</pre>
     *
     * @param charSequence the CharSequence whose string representation is to be written; {@code null} is written as the
     *                     four-character text {@code "null"}, matching {@link Appendable#append(CharSequence)}. The
     *                     {@code File} overloads instead treat {@code null} as "no content" - see
     *                     {@link #write(CharSequence, File)}.
     * @param output       the Writer where the CharSequence's string representation is to be written, must not be {@code null}.
     * @param flush        if {@code true}, the Writer is flushed after writing the CharSequence.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see #write(CharSequence, File)
     */
    public static void write(final CharSequence charSequence, final Writer output, final boolean flush) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        output.append(charSequence);

        if (flush) {
            output.flush();
        }
    }

    /**
     * Writes the byte array representation of a character array to a File using the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * File outputFile = new File("output.txt");
     * IOUtil.write(chars, outputFile);
     * }</pre>
     *
     * @param chars  the character array whose byte array representation is to be written; {@code null} is treated as empty.
     * @param output the File where the character array's byte array representation is to be written, must not be {@code null}.
     *               It is created, along with any missing parent directories, if it does not exist; an existing file is
     *               always truncated first, so writing a {@code null} or empty array leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #charsToBytes(char[], Charset)
     */
    public static void write(final char[] chars, final File output) throws IllegalArgumentException, IOException {
        write(chars, 0, N.len(chars), output);
    }

    /**
     * Writes the byte array representation of a character array to a File using the specified Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * File outputFile = new File("output.txt");
     * IOUtil.write(chars, StandardCharsets.UTF_8, outputFile);
     * }</pre>
     *
     * @param chars   the character array whose byte array representation is to be written; {@code null} is treated as empty.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param output  the File where the character array's byte array representation is to be written, must not be {@code null}.
     *                It is created, along with any missing parent directories, if it does not exist; an existing file is
     *                always truncated first, so writing a {@code null} or empty array leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #charsToBytes(char[], Charset)
     */
    public static void write(final char[] chars, final Charset charset, final File output) throws IllegalArgumentException, IOException {
        write(chars, 0, N.len(chars), charset, output);
    }

    /**
     * Writes the byte array representation of a portion of a character array to a File using the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * File outputFile = new File("output.txt");
     * IOUtil.write(chars, 1, 3, outputFile);  // Writes "ell"
     * }</pre>
     *
     * @param chars  the character array whose byte array representation is to be written; {@code null} is accepted only when {@code offset} and {@code count} are 0.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the File where the character array's byte array representation is to be written, must not be {@code null}.
     *               It is created, along with any missing parent directories, if it does not exist; an existing file is
     *               always truncated first, so a {@code count} of 0 leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code output} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #charsToBytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final File output)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        write(chars, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the byte array representation of a portion of a character array to a File using the specified Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * File outputFile = new File("output.txt");
     * IOUtil.write(chars, 1, 3, StandardCharsets.UTF_8, outputFile);  // Writes "ell"
     * }</pre>
     *
     * @param chars   the character array whose byte array representation is to be written; {@code null} is accepted only when {@code offset} and {@code count} are 0.
     * @param offset  the starting position in the character array.
     * @param count   the number of characters to be written from the character array.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param output  the File where the character array's byte array representation is to be written, must not be {@code null}.
     *                It is created, along with any missing parent directories, if it does not exist; an existing file is
     *                always truncated first, so a {@code count} of 0 leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code output} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     * @throws IOException if opening or writing {@code output} fails
     * @see #charsToBytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final File output)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        // The destination first, so a call that is wrong twice over reports it - as the byte[] twin and the
        // append(char[], int, int, File) mirror both do; charsToBytes would otherwise judge the range first and
        // answer IndexOutOfBoundsException where they answer IllegalArgumentException.
        N.checkArgNotNull(output, cs.output);

        // charsToBytes validates offset/count (negative and out-of-range alike) before the target file is opened,
        // so an invalid range can never truncate an existing file.
        write(charsToBytes(chars, offset, count, charset), output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * try (OutputStream output = new FileOutputStream("output.txt")) {
     *     IOUtil.write(chars, output);
     * }
     * }</pre>
     *
     * @param chars  the character array whose byte array representation is to be written.
     * @param output the OutputStream where the character array's byte array representation is to be written. It must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     */
    public static void write(final char[] chars, final OutputStream output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the specified Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * try (OutputStream output = new FileOutputStream("output.txt")) {
     *     IOUtil.write(chars, StandardCharsets.UTF_8, output);
     * }
     * }</pre>
     *
     * @param chars   the character array whose byte array representation is to be written.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param output  the OutputStream where the character array's byte array representation is to be written. It must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     */
    public static void write(final char[] chars, final Charset charset, final OutputStream output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, charset, output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * try (OutputStream os = new FileOutputStream("output.txt")) {
     *     IOUtil.write(chars, 0, 5, os);  // Writes "Hello"
     * }
     * }</pre>
     *
     * @param chars  the character array whose byte array representation is to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the OutputStream where the character array's byte array representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     * @throws IOException if writing to {@code output} fails
     * @see #charsToBytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final OutputStream output)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the specified Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * try (OutputStream os = new FileOutputStream("output.txt")) {
     *     IOUtil.write(chars, 0, 5, StandardCharsets.UTF_8, os);  // Writes "Hello" in UTF-8
     * }
     * }</pre>
     *
     * @param chars   the character array whose byte array representation is to be written.
     * @param offset  the starting position in the character array.
     * @param count   the number of characters to be written from the character array.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param output  the OutputStream where the character array's byte array representation is to be written, must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     * @throws IOException if writing to {@code output} fails
     * @see #charsToBytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final OutputStream output)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            return;
        }

        write(chars, offset, count, charset, output, false);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * try (OutputStream os = new FileOutputStream("output.txt")) {
     *     IOUtil.write(chars, os, true);  // Write and flush
     * }
     * }</pre>
     *
     * @param chars  the character array whose byte array representation is to be written.
     * @param output the OutputStream where the character array's byte array representation is to be written, must not be {@code null}.
     * @param flush  if {@code true}, the output stream is flushed after writing the character array.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see #charsToBytes(char[], Charset)
     */
    public static void write(final char[] chars, final OutputStream output, final boolean flush) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(chars)) {
            if (flush) {
                output.flush();
            }

            return;
        }

        write(chars, 0, chars.length, output, flush);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * try (OutputStream os = new FileOutputStream("output.txt")) {
     *     IOUtil.write(chars, 0, 5, os, true);  // Writes "Hello" and flushes
     * }
     * }</pre>
     *
     * @param chars  the character array whose byte array representation is to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the OutputStream where the character array's byte array representation is to be written, must not be {@code null}.
     * @param flush  if {@code true}, the output stream is flushed after writing the character array.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see #charsToBytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            if (flush) {
                output.flush();
            }

            return;
        }

        write(chars, offset, count, DEFAULT_CHARSET, output, flush);
    }

    /**
     * Writes the byte array representation of a character array to an OutputStream using the specified Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * try (OutputStream os = new FileOutputStream("output.txt")) {
     *     IOUtil.write(chars, 0, 5, StandardCharsets.UTF_8, os, true);  // Writes "Hello" in UTF-8 and flushes
     * }
     * }</pre>
     *
     * @param chars   the character array whose byte array representation is to be written.
     * @param offset  the starting position in the character array.
     * @param count   the number of characters to be written from the character array.
     * @param charset the Charset to be used to encode the character array into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param output  the OutputStream where the character array's byte array representation is to be written, must not be {@code null}.
     * @param flush   if {@code true}, the output stream is flushed after writing the character array.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     * @see #charsToBytes(char[], int, int, Charset)
     */
    public static void write(final char[] chars, final int offset, final int count, final Charset charset, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            if (flush) {
                output.flush();
            }

            return;
        }

        write(charsToBytes(chars, offset, count, charset), output, flush);
    }

    /**
     * Writes the string representation of a character array to a Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(chars, writer);
     * }
     * }</pre>
     *
     * @param chars  the character array to be written.
     * @param output the Writer where the character array is to be written. It must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     */
    public static void write(final char[] chars, final Writer output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(chars)) {
            return;
        }

        write(chars, 0, chars.length, output);
    }

    /**
     * Writes a portion of a character array to a Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(chars, 0, 5, writer);  // Writes "Hello"
     * }
     * }</pre>
     *
     * @param chars  the character array to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the Writer where the character array is to be written. It must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     * @throws IOException if writing to {@code output} fails
     */
    public static void write(final char[] chars, final int offset, final int count, final Writer output)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(chars, writer, true);  // Write and flush
     * }
     * }</pre>
     *
     * @param chars  the character array to be written.
     * @param output the Writer where the character array is to be written. It must not be {@code null}.
     * @param flush  if {@code true}, the output writer will be flushed after writing.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     */
    public static void write(final char[] chars, final Writer output, final boolean flush) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(chars)) {
            if (flush) {
                output.flush();
            }

            return;
        }

        write(chars, 0, chars.length, output, flush);
    }

    /**
     * Writes a portion of a character array to a Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd'};
     * try (Writer writer = new FileWriter("output.txt")) {
     *     IOUtil.write(chars, 0, 5, writer, true);  // Writes "Hello" and flushes
     * }
     * }</pre>
     *
     * @param chars  the character array to be written.
     * @param offset the starting position in the character array.
     * @param count  the number of characters to be written from the character array.
     * @param output the Writer where the character array is to be written. It must not be {@code null}.
     * @param flush  if {@code true}, the output writer will be flushed after writing.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     */
    public static void write(final char[] chars, final int offset, final int count, final Writer output, final boolean flush)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            if (flush) {
                output.flush();
            }

            return;
        }

        // Validate the range here rather than leaving it to Writer.write(..): a null array reached that call as
        // a NullPointerException, while the File and OutputStream forms of the same write reported the
        // documented IndexOutOfBoundsException. One operation must not report a bad range two different ways.
        N.checkFromIndexSize(offset, count, N.len(chars));

        output.write(chars, offset, count);

        if (flush) {
            output.flush();
        }
    }

    /**
     * Writes an array of bytes to a File.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
     * File output = new File("output.bin");
     * IOUtil.write(data, output);
     * }</pre>
     *
     * @param bytes  the byte array to be written; {@code null} is treated as empty.
     * @param output the File where the byte array is to be written. It is created, along with any missing parent
     *      directories, if it does not exist; an existing file is always truncated first, so writing a {@code null}
     *      or empty array leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if opening or writing {@code output} fails
     */
    public static void write(final byte[] bytes, final File output) throws IllegalArgumentException, IOException {
        write(bytes, 0, N.len(bytes), output);
    }

    /**
     * Writes a portion of a byte array to a File.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
     * File output = new File("output.bin");
     * IOUtil.write(data, 7, 5, output);  // Writes "World"
     * }</pre>
     *
     * @param bytes  the byte array to be written; {@code null} is accepted only when {@code offset} and {@code count} are 0.
     * @param offset the starting position in the byte array.
     * @param count  the number of bytes to be written from the byte array.
     * @param output the File where the byte array is to be written. It is created, along with any missing parent
     *      directories, if it does not exist; an existing file is always truncated first, so a {@code count} of 0
     *      leaves the file existing and empty.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code output} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code bytes}.
     * @throws IOException if opening or writing {@code output} fails
     */
    public static void write(final byte[] bytes, final int offset, final int count, final File output)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        // Validate the range before opening (and thus truncating) the target: otherwise a bad
        // offset/count would destroy an existing file and then fail with IndexOutOfBoundsException.
        N.checkFromIndexSize(offset, count, N.len(bytes));

        try (OutputStream os = openFileOutputStream(output)) {
            write(bytes, offset, count, os);
            os.flush();
        }
    }

    /**
     * Writes an array of bytes to an OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
     * try (OutputStream os = new FileOutputStream("output.bin")) {
     *     IOUtil.write(data, os);
     * }
     * }</pre>
     *
     * @param bytes  the byte array to be written.
     * @param output the OutputStream where the byte array is to be written. It must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} fails
     */
    public static void write(final byte[] bytes, final OutputStream output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(bytes)) {
            return;
        }

        write(bytes, 0, bytes.length, output);
    }

    /**
     * Writes a portion of a byte array to an OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
     * try (OutputStream os = new FileOutputStream("output.bin")) {
     *     IOUtil.write(data, 7, 5, os);  // Writes "World"
     * }
     * }</pre>
     *
     * @param bytes  the byte array to be written.
     * @param offset the starting position in the byte array.
     * @param count  the number of bytes to be written from the byte array.
     * @param output the OutputStream where the byte array is to be written. It must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code bytes}.
     * @throws IOException if writing to {@code output} fails
     */
    public static void write(final byte[] bytes, final int offset, final int count, final OutputStream output)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Data".getBytes(StandardCharsets.UTF_8);
     * try (OutputStream os = new FileOutputStream("output.bin")) {
     *     IOUtil.write(data, os, true);  // Write and flush
     * }
     * }</pre>
     *
     * @param bytes  the byte array to be written.
     * @param output the OutputStream where the byte array is to be written. It must not be {@code null}.
     * @param flush  if {@code true}, the output stream is flushed after writing the byte array.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     */
    public static void write(final byte[] bytes, final OutputStream output, final boolean flush) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(output, cs.output);

        if (N.isEmpty(bytes)) {
            if (flush) {
                output.flush();
            }

            return;
        }

        write(bytes, 0, bytes.length, output, flush);
    }

    /**
     * Writes a portion of a byte array to an OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
     * try (OutputStream os = new FileOutputStream("output.bin")) {
     *     IOUtil.write(data, 0, 5, os, true);  // Writes "Hello" and flushes
     * }
     * }</pre>
     *
     * @param bytes  the byte array to be written.
     * @param offset the starting position in the byte array.
     * @param count  the number of bytes to be written from the byte array.
     * @param output the OutputStream where the byte array is to be written. It must not be {@code null}.
     * @param flush  if {@code true}, the output stream is flushed after writing the byte array.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code bytes}.
     * @throws IOException if writing to {@code output} or a requested flush fails
     */
    public static void write(final byte[] bytes, final int offset, final int count, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(bytes) >= offset) {
            if (flush) {
                output.flush();
            }

            return;
        }

        // See write(char[], int, int, Writer, boolean): OutputStream.write(..) reports a null array as a
        // NullPointerException, not as the IndexOutOfBoundsException this family documents.
        N.checkFromIndexSize(offset, count, N.len(bytes));

        output.write(bytes, offset, count);

        if (flush) {
            output.flush();
        }
    }

    /**
     * Writes the content of the source file to the output file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("source.bin");
     * File output = new File("output.bin");
     * long bytesWritten = IOUtil.write(source, output);
     * }</pre>
     *
     * <p><b>{@code write} or {@code copyFile}?</b> Both replace {@code output} with the bytes of {@code source}, but
     * they are not interchangeable:
     * <ul>
     *   <li>{@link #copyFile(File, File)} is the file-copy operation: it copies through
     *       {@link Files#copy(Path, Path, CopyOption...)} and <b>preserves the source's timestamps</b>, failing if
     *       it cannot. Use it when the result should be a faithful copy.</li>
     *   <li>{@code write} is the byte-stream operation: it streams the content and returns how many bytes were
     *       written, leaving the destination with a <b>fresh modification time</b>. Use it when you want a count,
     *       or when only a slice of the source is wanted - see {@link #write(File, long, long, File)}, which
     *       {@code copyFile} has no equivalent of.</li>
     * </ul>
     *
     * @param source the file to read from.
     * @param output the file to write to.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of bytes written to the output file.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}, or if the two are the
     *         same file.
     * @throws IOException if reading from {@code source} or opening or writing {@code output} fails
     * @see #copyFile(File, File)
     * @see #write(File, long, long, File)
     */
    public static long write(final File source, final File output) throws IllegalArgumentException, IOException {
        return write(source, 0, Long.MAX_VALUE, output);
    }

    /**
     * Writes a portion of a file to another file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("large_file.bin");
     * File output = new File("partial.bin");
     * long bytesWritten = IOUtil.write(source, 1024, 2048, output);  // Copy bytes 1024-3071
     * }</pre>
     *
     * @param source the source file to be written, must not be {@code null}.
     * @param offset the starting position in the source file, in bytes.
     * @param count  the number of bytes to be written from the source file.
     * @param output the output file where the source file is to be written, must not be {@code null}.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of bytes written.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code source} and
     *         {@code output} are the same file, or if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or opening or writing {@code output} fails
     */
    public static long write(final File source, final long offset, final long count, final File output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        // Guard against a self-copy: opening output truncates it to 0 before source is read, wiping the
        // file and returning 0 (data loss). Peer copyFile(...) rejects the same-file case identically.
        requireCanonicalPathsNotEquals(source, output);

        try (InputStream is = openFileInputStream(source);
             OutputStream os = openFileOutputStream(output)) {
            return write(is, offset, count, os, true);
        }
    }

    /**
     * Writes the content of a source file to an {@code OutputStream}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("data.bin");
     * try (OutputStream os = new FileOutputStream("copy.bin")) {
     *     long bytesWritten = IOUtil.write(source, os);
     * }
     * }</pre>
     *
     * <p>{@link #copyFile(File, OutputStream)} is an alias for this method, kept because it reads better beside
     * the other {@code copyFile} overloads. Both stream the whole file and return the byte count; neither flushes
     * the stream unless asked - see {@link #write(File, OutputStream, boolean)}.
     *
     * @param source the source file to be written, must not be {@code null}.
     * @param output the {@code OutputStream} where the source file is to be written, must not be {@code null}.
     * @return the total number of bytes written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} fails
     * @see #copyFile(File, OutputStream)
     * @see #write(File, long, long, OutputStream)
     */
    public static long write(final File source, final OutputStream output) throws IllegalArgumentException, IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of the source file to the output stream, starting from the specified offset and writing up to the specified count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("large_file.bin");
     * try (OutputStream os = new FileOutputStream("partial.bin")) {
     *     long bytesWritten = IOUtil.write(source, 1000, 5000, os);
     * }
     * }</pre>
     *
     * @param source the file to read from.
     * @param offset the position in the file to start reading from.
     * @param count  the maximum number of bytes to write to the output.
     * @param output the output stream to write to. It must not be {@code null}.
     * @return the total number of bytes written to the output stream.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IOException if reading from {@code source} or writing to {@code output} fails
     */
    public static long write(final File source, final long offset, final long count, final OutputStream output) throws IllegalArgumentException, IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of a source file to an {@code OutputStream}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("data.bin");
     * try (OutputStream os = new FileOutputStream("copy.bin")) {
     *     long bytesWritten = IOUtil.write(source, os, true);  // Write and flush
     * }
     * }</pre>
     *
     * @param source the source file to be written, must not be {@code null}.
     * @param output the {@code OutputStream} where the source file is to be written, must not be {@code null}.
     * @param flush  if {@code true}, the output stream will be flushed after the write operation.
     * @return the total number of bytes written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} or a requested flush fails
     */
    public static long write(final File source, final OutputStream output, final boolean flush) throws IllegalArgumentException, IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes the content of the source file to the output stream, starting from the specified offset and writing up to the specified count.
     * If the flush parameter is {@code true}, the output stream is flushed after the write operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("large_file.bin");
     * try (OutputStream os = new FileOutputStream("partial.bin")) {
     *     long bytesWritten = IOUtil.write(source, 100, 500, os, true);  // Write 500 bytes from offset 100
     * }
     * }</pre>
     *
     * @param source the file to read from.
     * @param offset the position in the file to start reading from.
     * @param count  the maximum number of bytes to write to the output.
     * @param output the output stream to write to. It must not be {@code null}.
     * @param flush  if {@code true}, the output stream is flushed after the write operation.
     * @return the total number of bytes written to the output stream.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}, or if {@code offset} or {@code count} is negative.
     * @throws IOException if reading from {@code source} or writing to {@code output} or a requested flush fails
     */
    public static long write(final File source, final long offset, final long count, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        // Validated before the source is opened: otherwise a missing target costs a file handle, and a call
        // that is wrong twice over reports the source's absence rather than the argument that is actually null.
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        try (InputStream is = openFileInputStream(source)) {
            return write(is, offset, count, output, flush);
        }
    }

    /**
     * Writes the content of the input stream to the specified output file.
     *
     * <p>This overload cannot tell that {@code source} reads from {@code output}: a caller-supplied stream
     * carries no path. Its {@code File}-typed twin {@link #write(File, File)} rejects that case; here the
     * output is opened, and therefore truncated, before the first byte is read, so
     * {@code write(new FileInputStream(f), f)} empties {@code f} and returns 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("source.bin")) {
     *     File output = new File("output.bin");
     *     long bytesWritten = IOUtil.write(is, output);
     * }
     * }</pre>
     *
     * @param source the input stream to read from.
     * @param output the file to write to.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of bytes written to the output file.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or opening or writing {@code output} fails
     */
    public static long write(final InputStream source, final File output) throws IllegalArgumentException, IOException {
        return write(source, 0, Long.MAX_VALUE, output);
    }

    /**
     * Writes the content of an {@code InputStream} to a file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("source.bin")) {
     *     File output = new File("partial.bin");
     *     long bytesWritten = IOUtil.write(is, 512, 1024, output);
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to be written, must not be {@code null}.
     * @param offset the starting point from where to begin writing bytes from the {@code InputStream}, in bytes.
     * @param count  the maximum number of bytes to write to the file.
     * @param output the file where the {@code InputStream} is to be written, must not be {@code null}.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of bytes written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}, or if {@code offset} or
     *         {@code count} is negative.
     * @throws IOException if reading from {@code source} or opening or writing {@code output} fails
     */
    public static long write(final InputStream source, final long offset, final long count, final File output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        try (OutputStream os = openFileOutputStream(output)) {
            final long result = write(source, offset, count, os);
            os.flush();
            return result;
        }
    }

    /**
     * Writes the content of an {@code InputStream} to an {@code OutputStream}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("source.bin");
     *      OutputStream os = new FileOutputStream("dest.bin")) {
     *     long bytesWritten = IOUtil.write(is, os);
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to be written, must not be {@code null}.
     * @param output the {@code OutputStream} where the {@code InputStream} is to be written, must not be {@code null}.
     * @return the total number of bytes written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} fails
     */
    public static long write(final InputStream source, final OutputStream output) throws IllegalArgumentException, IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of an {@code InputStream} to an {@code OutputStream}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("source.bin");
     *      OutputStream os = new FileOutputStream("partial.bin")) {
     *     long bytesWritten = IOUtil.write(is, 256, 512, os);
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to be written, must not be {@code null}.
     * @param offset the starting point from where to begin writing bytes from the {@code InputStream}, in bytes.
     * @param count  the maximum number of bytes to write to the {@code OutputStream}.
     * @param output the {@code OutputStream} where the {@code InputStream} is to be written, must not be {@code null}.
     * @return the total number of bytes written.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code source} or
     *         {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} fails
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output)
            throws IllegalArgumentException, IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of an {@code InputStream} to an {@code OutputStream}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("source.bin");
     *      OutputStream os = new FileOutputStream("dest.bin")) {
     *     long bytesWritten = IOUtil.write(is, os, true);  // Write and flush
     * }
     * }</pre>
     *
     * @param source the {@code InputStream} to be written, must not be {@code null}.
     * @param output the {@code OutputStream} where the {@code InputStream} is to be written, must not be {@code null}.
     * @param flush  if {@code true}, the output stream will be flushed after writing.
     * @return the total number of bytes written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} or a requested flush fails
     */
    public static long write(final InputStream source, final OutputStream output, final boolean flush) throws IllegalArgumentException, IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes the content of the input stream to the output stream, starting from the specified offset and writing up to the specified count.
     * If the flush parameter is {@code true}, the output stream is flushed after the write operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("source.bin");
     *      OutputStream os = new FileOutputStream("partial.bin")) {
     *     long bytesWritten = IOUtil.write(is, 100, 500, os, true);
     * }
     * }</pre>
     *
     * @param source the input stream to read from.
     * @param offset the position in the input stream to start reading from.
     * @param count  the maximum number of bytes to write to the output.
     * @param output the output stream to write to.
     * @param flush  if {@code true}, the output stream is flushed after the write operation.
     * @return the total number of bytes written to the output stream. If the source holds fewer than {@code offset}
     *         bytes, nothing is written and {@code 0} is returned - indistinguishable from a source that had exactly
     *         {@code offset} bytes and nothing after them. A {@code count} of 0 returns 0 without touching the
     *         source at all, so nothing is skipped either - matching {@link #readBytes(InputStream, long, int)}.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code source} or
     *         {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} or a requested flush fails
     */
    public static long write(final InputStream source, final long offset, final long count, final OutputStream output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        // Decided BEFORE the skip: a count of 0 asks for nothing, and the read twins (readBytes/readChars) answer
        // it without moving the source. This used to skip `offset` first and only then return 0, so the same
        // (offset, 0) pair consumed the source on the write side and left it alone on the read side.
        if (count == 0) {
            if (flush) {
                output.flush();
            }

            return 0;
        }

        final byte[] buf = Objectory.createByteArrayBuffer();

        try {
            if (offset > 0) {
                final long skipped = skip(source, offset);

                if (skipped < offset) {
                    if (flush) {
                        output.flush();
                    }

                    return 0;
                }
            }

            final int bufLength = buf.length;
            long totalCount = 0;
            int cnt = 0;

            while ((totalCount < count) && (EOF != (cnt = read(source, buf, 0, (int) Math.min(count - totalCount, bufLength))))) {
                if (cnt == 0) {
                    break;
                }

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
     * Writes the content of a Reader to a file using the default charset. If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     *
     * <p>This overload cannot tell that {@code source} reads from {@code output}: a caller-supplied reader
     * carries no path. Its {@code File}-typed twin {@link #write(File, File)} rejects that case; here the
     * output is opened, and therefore truncated, before the first character is read, so writing a file's own
     * reader back to it empties the file and returns 0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("source.txt")) {
     *     File output = new File("output.txt");
     *     long charsWritten = IOUtil.write(reader, output);
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to be written, must not be {@code null}.
     * @param output the file where the {@code Reader}'s content is to be written, must not be {@code null}.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or opening or writing {@code output} fails
     */
    public static long write(final Reader source, final File output) throws IllegalArgumentException, IOException {
        return write(source, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the content of a Reader to a file using the specified Charset. If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("source.txt")) {
     *     File output = new File("output.txt");
     *     long charsWritten = IOUtil.write(reader, StandardCharsets.UTF_8, output);
     * }
     * }</pre>
     *
     * @param source  the {@code Reader} to be written, must not be {@code null}.
     * @param charset the {@code Charset} to be used to open the specified file for writing. If {@code null}, the default charset (UTF-8) is used.
     * @param output  the file where the {@code Reader}'s content is to be written, must not be {@code null}.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or opening or writing {@code output} fails
     */
    public static long write(final Reader source, final Charset charset, final File output) throws IllegalArgumentException, IOException {
        return write(source, 0, Long.MAX_VALUE, charset, output);
    }

    /**
     * Writes the content of a Reader to a file starting from a specific offset and up to a certain count using the default charset. If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("source.txt")) {
     *     File output = new File("output.txt");
     *     long charsWritten = IOUtil.write(reader, 100, 500, output);
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to be written, must not be {@code null}.
     * @param offset the position in the {@code Reader} to start writing from, in characters.
     * @param count  the maximum number of characters to be written.
     * @param output the file where the {@code Reader}'s content is to be written, must not be {@code null}.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}, or if {@code offset} or
     *         {@code count} is negative.
     * @throws IOException if reading from {@code source} or opening or writing {@code output} fails
     */
    public static long write(final Reader source, final long offset, final long count, final File output) throws IllegalArgumentException, IOException {
        return write(source, offset, count, DEFAULT_CHARSET, output);
    }

    /**
     * Writes the content of a Reader to a file starting from a specific offset and up to a certain count using the specified Charset. If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("source.txt")) {
     *     File output = new File("output.txt");
     *     long charsWritten = IOUtil.write(reader, 100, 500, StandardCharsets.UTF_8, output);
     * }
     * }</pre>
     *
     * @param source  the {@code Reader} to be written, must not be {@code null}.
     * @param offset  the position in the {@code Reader} to start writing from, in characters.
     * @param count   the maximum number of characters to be written.
     * @param charset the {@code Charset} to be used to open the specified file for writing. If {@code null}, the default charset (UTF-8) is used.
     * @param output  the file where the {@code Reader}'s content is to be written, must not be {@code null}.
     *      If the file exists, it will be overwritten. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}, or if {@code offset} or
     *         {@code count} is negative.
     * @throws IOException if reading from {@code source} or opening or writing {@code output} fails
     */
    public static long write(final Reader source, final long offset, final long count, final Charset charset, final File output)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        try (Writer writer = openFileWriter(output, checkCharset(charset))) {
            final long result = write(source, offset, count, writer);
            writer.flush();
            return result;
        }
    }

    /**
     * Writes the content from a {@code Reader} to a {@code Writer}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("source.txt");
     *      Writer writer = new FileWriter("dest.txt")) {
     *     long charsWritten = IOUtil.write(reader, writer);
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to be written, must not be {@code null}.
     * @param output the {@code Writer} where the {@code Reader}'s content is to be written, must not be {@code null}.
     * @return the total number of characters written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} fails
     */
    public static long write(final Reader source, final Writer output) throws IllegalArgumentException, IOException {
        return write(source, output, false);
    }

    /**
     * Writes the content of a Reader to a Writer starting from a specific offset and up to a certain count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("source.txt");
     *      Writer writer = new FileWriter("dest.txt")) {
     *     long charsWritten = IOUtil.write(reader, 100, 500, writer);
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to be written, must not be {@code null}.
     * @param offset the position in the {@code Reader} to start writing from, in characters.
     * @param count  the maximum number of characters to be written.
     * @param output the {@code Writer} where the {@code Reader}'s content is to be written, must not be {@code null}.
     * @return the total number of characters written.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code source} or
     *         {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} fails
     */
    public static long write(final Reader source, final long offset, final long count, final Writer output) throws IllegalArgumentException, IOException {
        return write(source, offset, count, output, false);
    }

    /**
     * Writes the content of a {@code Reader} to a {@code Writer}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("source.txt");
     *      Writer writer = new FileWriter("dest.txt")) {
     *     long charsWritten = IOUtil.write(reader, writer, true);  // Write and flush
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to be written, must not be {@code null}.
     * @param output the {@code Writer} where the {@code Reader}'s content is to be written, must not be {@code null}.
     * @param flush  if {@code true}, the output {@code Writer} is flushed after writing.
     * @return the total number of characters written.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws IOException if reading from {@code source} or writing to {@code output} or a requested flush fails
     */
    public static long write(final Reader source, final Writer output, final boolean flush) throws IllegalArgumentException, IOException {
        return write(source, 0, Long.MAX_VALUE, output, flush);
    }

    /**
     * Writes a specified number of characters from a Reader to a Writer, starting from a specified offset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("source.txt");
     *      Writer writer = new FileWriter("dest.txt")) {
     *     long charsWritten = IOUtil.write(reader, 100, 500, writer, true);
     * }
     * }</pre>
     *
     * @param source the Reader to read from.
     * @param offset the position in the Reader to start reading from.
     * @param count  the maximum number of characters to read from the Reader and write to the Writer.
     * @param output the Writer to write to.
     * @param flush  if {@code true}, the output Writer is flushed after writing.
     * @return the total number of characters written to the Writer. If the source holds fewer than {@code offset}
     *         characters, nothing is written and {@code 0} is returned - indistinguishable from a source that had
     *         exactly {@code offset} characters and nothing after them. A {@code count} of 0 returns 0 without
     *         touching the source at all, so nothing is skipped either - matching
     *         {@link #readChars(Reader, long, int)}.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code source} or
     *         {@code output} is {@code null}.
     * @throws IOException              if reading from {@code source} or writing to {@code output} or a requested flush fails
     */
    public static long write(final Reader source, final long offset, final long count, final Writer output, final boolean flush)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        // See write(InputStream, long, long, OutputStream, boolean): a count of 0 never moves the source.
        if (count == 0) {
            if (flush) {
                output.flush();
            }

            return 0;
        }

        final char[] buf = Objectory.createCharArrayBuffer();

        try {
            if (offset > 0) {
                final long skipped = skip(source, offset);

                if (skipped < offset) {
                    if (flush) {
                        output.flush();
                    }

                    return 0;
                }
            }

            final int bufLength = buf.length;
            long totalCount = 0;
            int cnt = 0;

            while ((totalCount < count) && (EOF != (cnt = read(source, buf, 0, (int) Math.min(count - totalCount, bufLength))))) {
                if (cnt == 0) {
                    break;
                }

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
     * Appends the given byte array to the specified file. If the file exists, data will be appended. If the file's parent directory doesn't exist, it will be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Additional data\n".getBytes(StandardCharsets.UTF_8);
     * File file = new File("log.txt");
     * IOUtil.append(data, file);  // Appends to existing file
     * }</pre>
     *
     * @param bytes      the byte array to append to the file.
     * @param targetFile the file to which the byte array will be appended.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     */
    public static void append(final byte[] bytes, final File targetFile) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);

        if (N.isEmpty(bytes)) {
            openAppendTargetOnly(targetFile);
            return;
        }

        append(bytes, 0, bytes.length, targetFile);
    }

    /**
     * Appends a portion of a byte array to the specified file. If the file exists, data will be appended. If the file's parent directory doesn't exist, it will be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
     * File file = new File("log.txt");
     * IOUtil.append(data, 7, 5, file);  // Appends "World"
     * }</pre>
     *
     * @param bytes      the byte array to append to the file.
     * @param offset     the starting index from where to append the bytes.
     * @param count      the number of bytes to append from the byte array.
     * @param targetFile the file to which the byte array will be appended.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code targetFile} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code bytes}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     */
    public static void append(final byte[] bytes, final int offset, final int count, final File targetFile)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(bytes) >= offset) {
            openAppendTargetOnly(targetFile);
            return;
        }

        // Validate the range before creating/opening the target so a bad offset/count fails fast
        // instead of leaving a freshly-created empty file behind (mirrors write(byte[], ...)).
        N.checkFromIndexSize(offset, count, N.len(bytes));

        try (OutputStream output = openFileOutputStream(targetFile, true)) {
            write(bytes, offset, count, output, true);
        }
    }

    /**
     * Appends the given character array to the specified file using the default charset. If the file exists, data will be appended. If the file's parent directory doesn't exist, it will be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Additional text\n".toCharArray();
     * File file = new File("log.txt");
     * IOUtil.append(chars, file);
     * }</pre>
     *
     * @param chars      the character array to append to the file.
     * @param targetFile the file to which the character array will be appended.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #charsToBytes(char[], Charset)
     */
    public static void append(final char[] chars, final File targetFile) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);

        if (N.isEmpty(chars)) {
            openAppendTargetOnly(targetFile);
            return;
        }

        append(chars, 0, chars.length, targetFile);
    }

    /**
     * Appends the given character array to the specified file using the specified Charset. If the file exists, data will be appended. If the file's parent directory doesn't exist, it will be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Additional text\n".toCharArray();
     * File file = new File("log.txt");
     * IOUtil.append(chars, StandardCharsets.UTF_8, file);
     * }</pre>
     *
     * @param chars      the character array to append to the file.
     * @param charset    the Charset to be used to encode the character array into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param targetFile the file to which the character array will be appended.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #charsToBytes(char[], Charset)
     */
    public static void append(final char[] chars, final Charset charset, final File targetFile) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);

        if (N.isEmpty(chars)) {
            openAppendTargetOnly(targetFile);
            return;
        }

        append(chars, 0, chars.length, charset, targetFile);
    }

    /**
     * Appends the content of the character array to the target file using the default charset.
     * The content to be appended starts at the specified offset and extends count characters. If the file exists, data will be appended. If the file's parent directory doesn't exist, it will be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "Hello, World!".toCharArray();
     * File file = new File("log.txt");
     * IOUtil.append(chars, 7, 5, file);  // Appends "World"
     * }</pre>
     *
     * @param chars      the character array to append to the file.
     * @param offset     the initial offset in the character array.
     * @param count      the number of characters to append.
     * @param targetFile the file to which the character array will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code targetFile} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     *         The range is validated before the target is created or opened, so a bad range never leaves a
     *         freshly created empty file behind.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #charsToBytes(char[], int, int, Charset)
     */
    public static void append(final char[] chars, final int offset, final int count, final File targetFile)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            openAppendTargetOnly(targetFile);
            return;
        }

        append(chars, offset, count, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of the character array to the target file.
     * The content to be appended starts at the specified offset and extends count characters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = File.createTempFile("data", ".txt");                            // may throw IOException
     * IOUtil.append("hello".toCharArray(), 0, 3, StandardCharsets.UTF_8, file);   // appends "hel"
     * IOUtil.append("hello".toCharArray(), 2, 0, StandardCharsets.UTF_8, file);   // count 0: no-op
     * String content = IOUtil.readAllToString(file);                              // returns "hel"
     * }</pre>
     *
     * @param chars      the character array to append to the file.
     * @param offset     the initial offset in the character array.
     * @param count      the number of characters to append.
     * @param charset    the Charset to be used to encode the character array into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param targetFile the file to which the character array will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code targetFile} is {@code null}.
     * @throws IndexOutOfBoundsException if {@code offset} and {@code count} exceed the length of {@code chars}.
     *         The range is validated before the target is created or opened, so a bad range never leaves a
     *         freshly created empty file behind.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #charsToBytes(char[], int, int, Charset)
     */
    public static void append(final char[] chars, final int offset, final int count, final Charset charset, final File targetFile)
            throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        if (count == 0 && N.len(chars) >= offset) {
            openAppendTargetOnly(targetFile);
            return;
        }

        append(charsToBytes(chars, offset, count, charset), targetFile);
    }

    /**
     * Appends the content of the CharSequence to the target file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * IOUtil.append("Hello World", log);   // appends "Hello World" to the file
     * IOUtil.append("", log);              // appends empty string (no change)
     * }</pre>
     *
     * @param cs         the CharSequence to append to the file; {@code null} or empty appends nothing (the file is
     *                   still created if it does not exist, and an existing file is never truncated).
     * @param targetFile the file to which the CharSequence will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     */
    public static void append(final CharSequence cs, final File targetFile) throws IllegalArgumentException, IOException {
        append(cs, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of the CharSequence to the target file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * IOUtil.append("Hello", StandardCharsets.UTF_8, log);   // appends using UTF-8
     * IOUtil.append("", StandardCharsets.UTF_16, log);       // appends empty string
     * }</pre>
     *
     * @param cs         the CharSequence to append to the file; {@code null} or empty appends nothing (the file is
     *                   still created if it does not exist, and an existing file is never truncated).
     * @param charset    the Charset to be used to encode the CharSequence into a sequence of bytes, if {@code null} the default charset (UTF-8) is used.
     * @param targetFile the file to which the CharSequence will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     */
    public static void append(final CharSequence cs, final Charset charset, final File targetFile) throws IllegalArgumentException, IOException {
        append(cs == null ? N.EMPTY_BYTE_ARRAY : toByteArray(cs, charset), targetFile);
    }

    /**
     * Appends the content of the source file to the target file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("snippet.txt");
     * File log = new File("log.txt");
     * long appended = IOUtil.append(source, log);  // Appends snippet.txt content to log.txt
     * }</pre>
     *
     * @param source     the source file to read from, must not be {@code null}.
     * @param targetFile the target file to append to, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @return the number of bytes appended.
     * @throws IllegalArgumentException if {@code source} and {@code targetFile} denote the same file (appending a file
     *         to itself would never terminate).
     * @throws IOException if reading from {@code source} or opening {@code targetFile} for append or writing the appended data fails
     */
    public static long append(final File source, final File targetFile) throws IllegalArgumentException, IOException {
        return append(source, 0, Long.MAX_VALUE, targetFile);
    }

    /**
     * Appends the content of the source file to the target file.
     * The content to be appended is read from the source file starting from the specified offset in bytes and up to the specified count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("data.bin");
     * File target = new File("archive.bin");
     * long appended = IOUtil.append(source, 1024, 4096, target);  // Append bytes 1024..5119 of source to target
     * }</pre>
     *
     * @param source     the source file to read from, must not be {@code null}.
     * @param offset     the starting point in bytes from where to read in the source file.
     * @param count      the maximum number of bytes to read from the source file.
     * @param targetFile the file to which the content will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @return the number of bytes appended to the target file.
     * @throws IllegalArgumentException if {@code offset} or {@code count} is negative, or if {@code source} and
     *         {@code targetFile} denote the same file, or if {@code source} or {@code targetFile} is {@code null}.
     * @throws IOException if reading from {@code source} or opening {@code targetFile} for append or writing the appended data fails
     */
    public static long append(final File source, final long offset, final long count, final File targetFile) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetFile, cs.targetFile);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        // Guard against a self-append: the target is opened in append mode while the same file is
        // still being read from the front, so the reader keeps finding the bytes the writer just
        // appended and the file grows without bound. Peer write(File, long, long, File) rejects
        // the same-file case identically.
        requireCanonicalPathsNotEquals(source, targetFile);

        try (InputStream is = openFileInputStream(source);
             OutputStream output = openFileOutputStream(targetFile, true)) {
            return write(is, offset, count, output, true);
        }
    }

    /**
     * Appends the content of the InputStream to the target file.
     * The content to be appended is read from the InputStream.
     *
     * <p>This overload cannot tell that {@code source} reads from {@code targetFile}: a caller-supplied stream
     * carries no path. Its {@code File}-typed twin {@link #append(File, File)} rejects that case; here the
     * target is opened in append mode while the same file is read from the front, so the reader keeps finding
     * the bytes just appended and {@code append(new FileInputStream(f), f)} grows {@code f} without bound.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = File.createTempFile("data", ".txt");   // may throw IOException
     * try (InputStream is = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8))) {
     *     long n = IOUtil.append(is, file);   // returns 3
     * }
     * String content = IOUtil.readAllToString(file);   // returns "abc"
     * }</pre>
     *
     * @param source     the InputStream to read from, must not be {@code null}.
     * @param targetFile the file to which the InputStream content will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @return the number of bytes appended to the target file.
     * @throws IllegalArgumentException if {@code source} or {@code targetFile} is {@code null}.
     * @throws IOException if reading from {@code source} or opening {@code targetFile} for append or writing the appended data fails
     */
    public static long append(final InputStream source, final File targetFile) throws IllegalArgumentException, IOException {
        return append(source, 0, Long.MAX_VALUE, targetFile);
    }

    /**
     * Appends the content of the InputStream to the target file.
     * The content to be appended is read from the InputStream starting from the specified offset in bytes and up to the specified count.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * byte[] data = "substring".getBytes(StandardCharsets.UTF_8);
     * try (InputStream is = new ByteArrayInputStream(data)) {
     *     IOUtil.append(is, 0, 3, log);                // appends first 3 bytes ("sub")
     *     IOUtil.append(is, 0, 6, log);                // returns 6 ("string" appended)
     * }
     * }</pre>
     *
     * @param source     the InputStream to read from, must not be {@code null}.
     * @param offset     the starting point in bytes from where to read in the InputStream.
     * @param count      the maximum number of bytes to read from the InputStream.
     * @param targetFile the file to which the InputStream content will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @return the number of bytes appended to the target file.
     * @throws IllegalArgumentException if {@code source} or {@code targetFile} is {@code null}, or if {@code offset}
     *         or {@code count} is negative.
     * @throws IOException if reading from {@code source} or opening {@code targetFile} for append or writing the appended data fails
     */
    public static long append(final InputStream source, final long offset, final long count, final File targetFile)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetFile, cs.targetFile);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        try (OutputStream output = openFileOutputStream(targetFile, true)) {
            return write(source, offset, count, output, true);
        }
    }

    /**
     * Appends all characters from the specified {@code Reader} to the target file using the default charset.
     *
     * <p>If the target file exists, content is appended; otherwise a new file is created.
     * Missing parent directories are created as needed. This method does not close the input reader.
     *
     * <p>This overload cannot tell that {@code source} reads from {@code targetFile}: a caller-supplied reader
     * carries no path. Its {@code File}-typed twin {@link #append(File, File)} rejects that case; here the
     * target is opened in append mode while the same file is read from the front, so appending a file's own
     * reader to it grows the file without bound.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new StringReader("New log entry\n")) {
     *     long appended = IOUtil.append(reader, new File("log.txt"));
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @param targetFile the file to append to, must not be {@code null}.
     * @return the number of characters appended to {@code targetFile}.
     * @throws IllegalArgumentException if {@code source} or {@code targetFile} is {@code null}.
     * @throws IOException if reading from {@code source} or opening {@code targetFile} for append or writing the appended data fails.
     */
    public static long append(final Reader source, final File targetFile) throws IllegalArgumentException, IOException {
        return append(source, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of a {@code Reader} to a file using the specified character set.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = File.createTempFile("data", ".txt");   // may throw IOException
     * try (Reader r = new StringReader("world")) {
     *     long n = IOUtil.append(r, StandardCharsets.UTF_8, file);   // returns 5
     * }
     * String content = IOUtil.readAllToString(file);   // returns "world"
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @param charset the character set to use for encoding, if {@code null} the default charset (UTF-8) is used.
     * @param targetFile the file where the {@code Reader}'s content is to be appended, must not be {@code null}.
     *                   If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters appended.
     * @throws IllegalArgumentException if {@code source} or {@code targetFile} is {@code null}.
     * @throws IOException if reading from {@code source} or opening {@code targetFile} for append or writing the appended data fails
     */
    public static long append(final Reader source, final Charset charset, final File targetFile) throws IllegalArgumentException, IOException {
        return append(source, 0, Long.MAX_VALUE, charset, targetFile);
    }

    /**
     * Appends the content of the Reader to the target file.
     * The content to be appended is read from the Reader starting from the specified offset in characters and up to the specified count.
     * The target file is opened in append mode using the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * try (Reader reader = new StringReader("hello world")) {
     *     IOUtil.append(reader, 0, 5, log);                 // appends first 5 chars ("hello")
     *     IOUtil.append(reader, 1, 5, log);                 // returns 5 ("world" appended after skipping the space)
     * }
     * }</pre>
     *
     * @param source     the Reader to read from, must not be {@code null}.
     * @param offset     the position in the Reader to start reading from.
     * @param count      the maximum number of characters to read from the Reader.
     * @param targetFile the file to which the Reader content will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @return the number of characters appended to the target file.
     * @throws IllegalArgumentException if {@code source} or {@code targetFile} is {@code null}, or if {@code offset}
     *         or {@code count} is negative.
     * @throws IOException if reading from {@code source} or opening {@code targetFile} for append or writing the appended data fails
     */
    public static long append(final Reader source, final long offset, final long count, final File targetFile) throws IllegalArgumentException, IOException {
        return append(source, offset, count, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the content of a {@code Reader} to a file using the specified character set, starting from a given offset and up to a specified count.
     * The input reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * try (Reader reader = new StringReader("hello world")) {
     *     IOUtil.append(reader, 0, 5, StandardCharsets.UTF_8, log);    // appends "hello" using UTF-8
     *     IOUtil.append(reader, 1, 5, StandardCharsets.UTF_8, log);    // returns 5 ("world" appended using UTF-8)
     * }
     * }</pre>
     *
     * @param source the {@code Reader} to read from, must not be {@code null}.
     * @param offset the starting position in characters from where to begin reading, must be &gt;= 0.
     * @param count the maximum number of characters to read, must be &gt;= 0.
     * @param charset the character set to use for encoding, if {@code null} the default charset (UTF-8) is used.
     * @param targetFile the file where the {@code Reader}'s content is to be appended, must not be {@code null}.
     *                   If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @return the total number of characters appended.
     * @throws IllegalArgumentException if {@code source} or {@code targetFile} is {@code null}, or if {@code offset}
     *         or {@code count} is negative.
     * @throws IOException if reading from {@code source} or opening {@code targetFile} for append or writing the appended data fails
     */
    public static long append(final Reader source, final long offset, final long count, final Charset charset, final File targetFile)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(targetFile, cs.targetFile);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        try (Writer writer = openFileWriter(targetFile, checkCharset(charset), true)) {
            final long result = write(source, offset, count, writer);
            writer.flush();
            return result;
        }
    }

    /**
     * Appends the string representation of the specified object as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * IOUtil.appendLine("Log entry", log);   // appends "Log entry" + newline using default charset
     * IOUtil.appendLine(12345, log);         // appends "12345" + newline using default charset
     * }</pre>
     *
     * @param obj        the object whose string representation is to be appended to the file.
     * @param targetFile the file to which the object's string representation will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #writeLine(Object, File)
     * @see N#toString(Object)
     */
    public static void appendLine(final Object obj, final File targetFile) throws IllegalArgumentException, IOException {
        appendLine(obj, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the string representation of the specified object as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     *
     * <p>Each call is its own encoding session, so a charset that writes a byte-order mark (such as UTF-16)
     * emits one per line appended. Collect the lines and use {@link #appendLines(Iterable, Charset, File)},
     * or append through a single long-lived {@code Writer}, for such a charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * IOUtil.appendLine("Log entry", StandardCharsets.UTF_8, log);   // appends "Log entry" + newline using UTF-8
     * IOUtil.appendLine(12345, StandardCharsets.UTF_8, log);         // appends "12345" + newline using UTF-8
     * }</pre>
     *
     * @param obj        the object whose string representation is to be appended to the file.
     * @param charset    the Charset to be used to encode string representation of the specified object into a sequence of bytes,
     *      if {@code null} the default charset (UTF-8) is used.
     * @param targetFile the file to which the object's string representation will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #writeLine(Object, File)
     * @see N#toString(Object)
     */
    public static void appendLine(final Object obj, final Charset charset, final File targetFile) throws IllegalArgumentException, IOException {
        final String str = N.toString(obj) + IOUtil.LINE_SEPARATOR_UNIX;

        append(toByteArray(str, charset), targetFile);
    }

    /**
     * Appends the string representation of each object in the provided iterable as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     * The file is opened in append mode using the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * List<String> entries = Arrays.asList("line1", "line2", "line3");
     * IOUtil.appendLines(entries, log);                           // appends all lines
     * IOUtil.appendLines(Collections.<String>emptyList(), log);   // empty list: no lines written (creates the file if it does not exist)
     * }</pre>
     *
     * @param lines      the iterable whose elements' string representations are to be appended to the file.
     * @param targetFile the file to which the elements' string representations will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #writeLines(Iterable, File)
     * @see N#toString(Object)
     */
    public static void appendLines(final Iterable<?> lines, final File targetFile) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);

        if (N.isEmptyCollection(lines)) {
            openAppendTargetOnly(targetFile);
            return;
        }

        appendLines(lines, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the string representation of each object in the provided iterable as a new line to the target file.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     * The file is opened in append mode using the provided charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * List<String> entries = Arrays.asList("line1", "line2", "line3");
     * IOUtil.appendLines(entries, StandardCharsets.UTF_8, log);                           // appends using UTF-8
     * IOUtil.appendLines(Collections.<String>emptyList(), StandardCharsets.UTF_8, log);   // empty list: no lines written (creates the file if it does not exist)
     * }</pre>
     *
     * @param lines      the iterable whose elements' string representations are to be appended to the file.
     * @param charset    the Charset to be used to open the specified file for writing, if {@code null} the default
     *      charset (UTF-8) is used.
     * @param targetFile the file to which the elements' string representations will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #writeLines(Iterable, File)
     * @see N#toString(Object)
     */
    public static void appendLines(final Iterable<?> lines, final Charset charset, final File targetFile) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);

        if (N.isEmptyCollection(lines)) {
            openAppendTargetOnly(targetFile);
            return;
        }

        try (Writer writer = openFileWriter(targetFile, checkCharset(charset), true)) {
            writeLines(lines, writer, true);
        }
    }

    /**
     * Appends the string representation of each object produced by the provided iterator as a new line to the
     * target file, using the default charset (UTF-8).
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * List<String> entries = Arrays.asList("line1", "line2", "line3");
     * IOUtil.appendLines(entries.iterator(), log);                            // appends all lines
     * IOUtil.appendLines(Collections.<String>emptyIterator(), log);           // no lines written (creates the file if it does not exist)
     * }</pre>
     *
     * @param lines      the iterator whose elements' string representations are to be appended to the file;
     *                   {@code null} or exhausted is treated as empty.
     * @param targetFile the file to which the elements' string representations will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #appendLines(Iterator, Charset, File)
     * @see #writeLines(Iterator, File)
     * @see N#toString(Object)
     */
    public static void appendLines(final Iterator<?> lines, final File targetFile) throws IllegalArgumentException, IOException {
        appendLines(lines, DEFAULT_CHARSET, targetFile);
    }

    /**
     * Appends the string representation of each object produced by the provided iterator as a new line to the
     * target file, using the specified Charset.
     * The string representation is obtained by invoking the {@code N.toString(Object)} method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * List<String> entries = Arrays.asList("line1", "line2", "line3");
     * IOUtil.appendLines(entries.iterator(), StandardCharsets.UTF_8, log);   // appends using UTF-8
     * }</pre>
     *
     * @param lines      the iterator whose elements' string representations are to be appended to the file;
     *                   {@code null} or exhausted is treated as empty.
     * @param charset    the Charset used to encode the lines, if {@code null} the default charset (UTF-8) is used.
     * @param targetFile the file to which the elements' string representations will be appended, must not be {@code null}.
     *      If the file exists, the content will be appended to it. If the file's parent directory doesn't exist, it will be created.
     * @throws IllegalArgumentException if {@code targetFile} is {@code null}.
     * @throws IOException if opening {@code targetFile} for append or writing the appended data fails
     * @see #writeLines(Iterator, Charset, File)
     * @see N#toString(Object)
     */
    public static void appendLines(final Iterator<?> lines, final Charset charset, final File targetFile) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);

        if (N.isEmpty(lines)) {
            openAppendTargetOnly(targetFile);
            return;
        }

        try (Writer writer = openFileWriter(targetFile, checkCharset(charset), true)) {
            writeLines(lines, writer, true);
        }
    }

    /**
     * Transfers bytes from a source channel to a target channel.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (ReadableByteChannel src = Channels.newChannel(new FileInputStream("src.dat"));
     *      WritableByteChannel dest = Channels.newChannel(new FileOutputStream("dest.dat"))) {
     *     long bytesTransferred = IOUtil.transfer(src, dest);
     * }
     * }</pre>
     *
     * <p><b>How much is transferred:</b> everything the source still holds. For a {@link FileChannel} source that
     * is {@code size() - position()}, captured once when the call starts; for any other source it is "until end
     * of input". A {@code FileChannel} whose {@code size()} is 0 - a FIFO, a character device such as
     * {@code /dev/stdin}, or simply an empty file - has no size to bound the transfer by and is read until end of
     * input like any other source. On the {@code FileChannel} path a transfer that cannot complete is reported as
     * an {@code IOException} rather than as a short return value, so a caller may treat the returned count as the
     * whole of the source; the one exception there is a source that <i>shrinks</i> during the call - it ends
     * earlier than the size it promised, and the count then says how much actually moved. The buffered path has
     * no promised size to measure against: it stops at end of input, or when the source repeatedly answers a read
     * with no bytes at all, and returns what actually moved - so check the count against the source yourself when
     * the source is not a {@code FileChannel}.
     *
     * <p>When both channels are {@link FileChannel}s and the source reports a size, the transfer is delegated to
     * {@link FileChannel#transferFrom(ReadableByteChannel, long, long)}, which lets the operating system move the
     * bytes without copying them through a user-space buffer. That call is permitted to make no progress on some
     * platforms, so a zero return falls back to an ordinary read/write at the channels' current positions rather
     * than ending the transfer - which would otherwise report a truncated copy as a successful one. Any other
     * pair is copied through a pooled buffer: for a non-file source a zero-byte transfer is ambiguous between
     * "no data right now" and "end of input", so the read loop - which retries a zero-byte read before giving
     * up on it, rather than telling the two apart - is used instead.
     *
     * <p>Bytes are written to the destination starting at <i>its</i> current position, and the destination's
     * position is left immediately after them.
     *
     * <p>Neither path forces the bytes to disk. The channel path hands them to the operating system and the
     * buffered path flushes the wrapping stream, but a durability guarantee needs
     * {@link FileChannel#force(boolean)} on the destination afterwards.
     *
     * @param src    the source channel from which bytes are to be read.
     * @param output the target channel to which bytes are to be written.
     * @return the number of bytes transferred, which is everything the source held unless it shrank during the
     *         call - or, on the buffered path, stopped making progress.
     * @throws IllegalArgumentException if {@code src} or {@code output} is {@code null}.
     * @throws IOException if reading {@code src} , writing {@code output} , or accessing a file-channel position or size fails, or if, on the
     *         {@code FileChannel} path, neither channel can make progress before the whole source has been moved. On the buffered path a source
     *         that stops making progress ends the transfer with a short return value instead, and a destination that stops accepting bytes fails
     *         with an unchecked exception from the wrapping stream.
     */
    public static long transfer(final ReadableByteChannel src, final WritableByteChannel output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(src, cs.src);
        N.checkArgNotNull(output, cs.output);

        // size() > 0: a FileChannel open on a FIFO, a pipe or a character device reports a size of 0, and the fast
        // path then either failed on the position probe (an lseek, "Illegal seek" on a pipe) or, for a device that
        // seeks, moved nothing and answered 0 - while the same source handed in as a stream is read to end of
        // input. That is the same test seekRegularFile(..) applies before trusting a channel's size, and an empty
        // regular file reads to end of input (zero bytes) just as quickly through the buffered path.
        if (src instanceof FileChannel && output instanceof FileChannel && ((FileChannel) src).size() > 0) {
            final FileChannel in = (FileChannel) src;
            final FileChannel dest = (FileChannel) output;

            // Bound the loop by the source's remaining size rather than by a zero return: that keeps the byte
            // count exact and makes termination independent of how short a single transferFrom happens to be.
            final long expected = in.size() - in.position();
            long remaining = expected;
            long position = dest.position();
            long total = 0;
            // Allocated lazily and reused: a platform where transferFrom never progresses would otherwise
            // allocate one buffer per 8 KB - and that platform is the whole reason the fallback exists.
            ByteBuffer buffer = null;

            while (remaining > 0) {
                final long transferred = dest.transferFrom(in, position, Math.min(remaining, FILE_COPY_BUFFER_SIZE));

                if (transferred > 0) {
                    position += transferred;
                    total += transferred;
                    remaining -= transferred;

                    continue;
                }

                // transferFrom is permitted to make no progress on some platforms - doCopyFile(..) says so in
                // as many words and has always handled it. Breaking out here instead reported a SHORT transfer
                // as a successful one, so a caller that trusted the return value silently lost the tail. Fall
                // back to an ordinary read/write, which - unlike doCopyFile's fallback - has to honour the two
                // channels' own positions rather than assuming a zero-based whole file.
                if (buffer == null) {
                    buffer = ByteBuffer.allocate(8192);
                }

                // limit(..), not a fresh allocation: never read more than the source promised is still there.
                buffer.clear().limit((int) Math.min(remaining, buffer.capacity()));

                final int bytesRead = in.read(buffer);

                if (bytesRead < 0) {
                    // The source ended before the size it reported: it shrank underneath us. Report what
                    // actually moved rather than an error about the caller's arguments.
                    break;
                } else if (bytesRead == 0) {
                    throw new IOException("Unable to make progress reading the source channel after " + total + " of " + expected + " bytes");
                }

                buffer.flip();

                while (buffer.hasRemaining()) {
                    final int written = dest.write(buffer, position);

                    if (written <= 0) {
                        throw new IOException("Unable to make progress writing to the destination channel after " + total + " of " + expected + " bytes");
                    }

                    position += written;
                    total += written;
                    remaining -= written;
                }
            }

            dest.position(position);

            return total;
        }

        return write(Channels.newInputStream(src), Channels.newOutputStream(output), true);
    }

    /**
     * Skips over and discards a specified number of bytes from the input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new ByteArrayInputStream("0123456789".getBytes(StandardCharsets.UTF_8))) {
     *     long skipped = IOUtil.skip(is, 3);   // returns 3
     *     int next = is.read();                // returns '3' (51)
     * }
     * try (InputStream is = new ByteArrayInputStream("ab".getBytes(StandardCharsets.UTF_8))) {
     *     long skipped = IOUtil.skip(is, 100); // returns 2 (capped at end of stream)
     *     long none = IOUtil.skip(is, 0);      // returns 0
     *     // IOUtil.skip(is, -1);              // throws IllegalArgumentException
     * }
     * }</pre>
     *
     * <p>Unlike {@link InputStream#skip(long)}, the return value does not count bytes past the end of the
     * stream, and this method never calls the stream's own {@code skip} - which is allowed to fail, and on a
     * source that cannot seek does exactly that. A {@link FileInputStream} open on a regular file is advanced by
     * moving its channel position, bounded by the file's size, so skipping a large offset costs a seek rather
     * than a copy. Every other source - a FIFO, a pipe, a character device, a decompressing stream, a socket, a
     * {@code BufferedInputStream}, a custom implementation - is advanced by reading and discarding, which works
     * for all of them.
     *
     * <p>The count is therefore exact for every stream, with one race left: a regular file that another process
     * truncates between this method measuring its size and moving the position can still make it report bytes
     * that are no longer there.
     *
     * <p><b>Cost:</b> the seek path is O(1) - skipping 64&nbsp;MB of a warm file measures ~0.01&nbsp;ms - while
     * the reading path is O(n), ~23&nbsp;ms for the same 64&nbsp;MB. That difference is worth knowing when the
     * source is a file: {@code skip(new FileInputStream(f), n)} seeks, but wrapping the same file in a
     * {@link java.io.BufferedInputStream} first does not, because a buffered stream's own {@code skip} may
     * delegate to a source that cannot seek and there is no way to tell from the outside. Pass the unwrapped
     * {@code FileInputStream} when the offset is large; buffer afterwards if you need to.
     *
     * @param input       the {@code InputStream} from which bytes are to be skipped, must not be {@code null}.
     * @param bytesToSkip the number of bytes to be skipped, must be &gt;= 0.
     * @return the actual number of bytes skipped, which may be less than {@code bytesToSkip} if the end of the stream is reached.
     * @throws IllegalArgumentException if {@code input} is {@code null}, or if {@code bytesToSkip} is negative.
     * @throws IOException if skipping or reading from {@code input} to advance its position fails
     * @see #skipFully(InputStream, long)
     */
    public static long skip(final InputStream input, final long bytesToSkip) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(input, cs.input);
        N.checkArgNotNegative(bytesToSkip, cs.bytesToSkip);

        if (bytesToSkip == 0) {
            return 0;
        }

        // Allocated lazily, as the Reader twin does: the seek fast path below satisfies the whole request
        // without ever reading, and this method's javadoc advertises that path as O(1) - taking a pooled 8 KB
        // buffer (and zero-filling it again on recycle) for a skip that never reads is pure cost.
        byte[] buf = null;
        long remain = bytesToSkip;

        try {
            while (remain > 0) {
                // Try to move without copying first. This used to read and discard every byte, which cost
                // 25 ms to skip 64 MB of a warm file where a seek costs nothing; see seekRegularFile(..)
                // for why only a regular file is moved this way and why the stream's own skip(..) is never used.
                final long seeked = seekRegularFile(input, remain);

                if (seeked > 0) {
                    remain -= seeked;
                    continue;
                }

                if (buf == null) {
                    buf = Objectory.createByteArrayBuffer();
                }

                final long n = read(input, buf, 0, (int) Math.min(remain, buf.length));

                if (n <= 0) { // EOF or no progress

                    break;
                }

                remain -= n;
            }

            return bytesToSkip - remain;
        } finally {
            if (buf != null) {
                Objectory.recycle(buf);
            }
        }
    }

    /**
     * Advances {@code input} by up to {@code remain} bytes by moving the file position - and only when the
     * stream is a {@link FileInputStream} open on something that has a position to move.
     *
     * <p><b>Why not {@link InputStream#skip(long)} on any stream that reports enough
     * {@link InputStream#available()}?</b> Because {@code skip} is allowed to fail, and on a source that cannot
     * seek it does: {@link FileInputStream#skip(long)} is implemented with {@code lseek}, which answers
     * {@code ESPIPE} for a FIFO, a pipe or a character device, so the call raises
     * {@code IOException: Illegal seek} for a source the read loop handles perfectly well. That failure was also
     * <i>size-dependent</i> - it appeared only once at least 8&nbsp;KB happened to be buffered - and it made
     * {@code readBytes(stream, offset, len)} throw on an input where {@code readChars(..)} over the same stream
     * succeeded, because the {@code Reader} side has always restricted its own fast path by type (see
     * {@link #skipsWithoutAReadLoop(Reader)}).
     *
     * <p><b>Catching that {@code IOException} and falling back would not be a fix.</b> {@code skip} may advance
     * part of the way and only then fail, and the caller cannot find out how far it got, so re-reading the full
     * remainder would silently skip too much. This method therefore never calls {@code skip} at all. Moving the
     * channel position is <i>atomic</i> - it either takes effect or throws without moving - and it is bounded by
     * {@link FileChannel#size()}, so unlike {@code FileInputStream.skip}, which is explicitly allowed to "skip
     * more bytes than what are remaining in the backing file", it can never move past the end.
     * ({@link InputStream#skipNBytes(long)} inherits that same blind spot, which is why it is not used either.)
     *
     * <p>A size of {@code 0} identifies exactly the sources that cannot be moved - a FIFO, a pipe, a character
     * device - as well as an empty file, and answers {@code 0} so the caller reads instead. That check comes
     * before {@link FileChannel#position()}, which is itself an {@code lseek} and would fail on those sources.
     *
     * <p>This still covers every source the fast path exists for: the sliced reads that motivated it
     * ({@code readBytes(File, offset, ..)}, {@code write(File, offset, count, ..)}, ...) all open through
     * {@link #openFileInputStream(File)} and hand a plain {@code FileInputStream} to this method. Skipping
     * 64&nbsp;MB of a warm file costs microseconds instead of copying 64&nbsp;MB through a buffer. Everything
     * else - a decompressing stream, a socket, a {@code BufferedInputStream}, a custom implementation - reads
     * and discards, which is correct for all of them and was already the only path a {@code GZIPInputStream}
     * could take (it reports {@code available() == 1} whenever it is merely "not at EOF").
     *
     * @param input  the stream to advance.
     * @param remain the number of bytes still wanted; must be positive.
     * @return the number of bytes skipped, possibly {@code 0} if the caller should read instead.
     * @throws IOException if reading or updating the file-channel position, or obtaining the file size, fails.
     *         A closed {@code FileInputStream} reports {@link java.nio.channels.ClosedChannelException} from the size probe rather than the
     *         {@code IOException("Stream Closed")} that {@code available()} used to raise; both are
     *         {@code IOException}s, so {@link #skip(InputStream, long)}'s declared contract is unchanged.
     */
    private static long seekRegularFile(final InputStream input, final long remain) throws IOException {
        if (!(input instanceof FileInputStream)) {
            return 0;
        }

        final FileChannel channel = ((FileInputStream) input).getChannel();
        final long size = channel.size();

        if (size <= 0) {
            // A FIFO, a pipe or a character device (all of which report 0), or simply an empty file: there is
            // no position to move within, and asking for one would fail. Let the caller read instead.
            return 0;
        }

        final long position = channel.position();

        if (position >= size) {
            return 0;
        }

        // Compared as a remaining-distance rather than as Math.min(position + remain, size): remain can be
        // Long.MAX_VALUE (readAllBytes and the unbounded write(..) forms pass it straight through), and adding
        // that to a non-zero position wraps negative, which channel.position(..) then rejects outright.
        // Both operands here are non-negative and position < size, so neither side can overflow.
        final long target = remain >= size - position ? size : position + remain;

        // A FileInputStream and its channel share one file position, so this advances the stream itself.
        channel.position(target);

        return target - position;
    }

    /**
     * Skips over and discards a specified number of characters from the input reader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new StringReader("abcdefghij")) {
     *     long skipped = IOUtil.skip(reader, 3);         // skips "abc", returns 3
     *     char[] buf = new char[7];
     *     IOUtil.read(reader, buf);                      // reads "defghij"
     * }
     * try (Reader reader = new StringReader("abc")) {
     *     long skipped = IOUtil.skip(reader, 10);        // skips all 3 chars, returns 3 (not 10)
     * }
     * }</pre>
     *
     * <p>A {@link java.io.BufferedReader}, {@link java.io.StringReader} or {@link java.io.CharArrayReader} is
     * advanced with its own {@code skip}. For the last two that is a pointer move rather than a copy, so skipping
     * a large offset costs almost nothing; a {@code BufferedReader} moves a pointer within the characters it
     * already holds and refills from its source - a copy, but a buffered one - past them. Any other reader is
     * advanced by reading and discarding. The result is exact
     * either way: unlike {@link InputStream#skip(long)}, {@code Reader.skip(long)} is specified to return the
     * number of characters <i>actually</i> skipped and cannot move past the end.
     *
     * @param input       the {@code Reader} from which characters are to be skipped, must not be {@code null}.
     * @param charsToSkip the number of characters to be skipped, must be &gt;= 0.
     * @return the actual number of characters skipped, which may be less than {@code charsToSkip} if the end of the reader is reached.
     * @throws IllegalArgumentException if {@code input} is {@code null}, or if {@code charsToSkip} is negative.
     * @throws IOException if skipping or reading from {@code input} to advance its position fails
     * @see #skipFully(Reader, long)
     */
    public static long skip(final Reader input, final long charsToSkip) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(input, cs.input);
        N.checkArgNotNegative(charsToSkip, cs.charsToSkip);

        if (charsToSkip == 0) {
            return 0;
        }

        long remain = charsToSkip;
        final boolean canSeek = skipsWithoutAReadLoop(input);
        char[] buf = null;

        try {
            while (remain > 0) {
                if (canSeek) {
                    // Exact by contract: Reader.skip(..) "returns the number of characters actually skipped" and
                    // has none of the licence FileInputStream.skip(..) has to move past the end, so it can be
                    // trusted with the full count - unlike the InputStream side (see seekRegularFile(..)).
                    // For these three types it is a pointer move: skipping 4M characters measured 889us through
                    // the read loop below and 1us through skip(..).
                    final long skipped = input.skip(remain);

                    if (skipped > 0) {
                        remain -= skipped;
                        continue;
                    }

                    // 0 means end of input for these types, so there is nothing left to fall back to.
                    break;
                }

                if (buf == null) {
                    buf = Objectory.createCharArrayBuffer();
                }

                final long n = read(input, buf, 0, (int) Math.min(remain, buf.length));

                if (n <= 0) { // EOF or no progress

                    break;
                }

                remain -= n;
            }

            return charsToSkip - remain;
        } finally {
            if (buf != null) {
                Objectory.recycle(buf);
            }
        }
    }

    /**
     * Whether {@code reader}'s {@link Reader#skip(long)} is a real seek rather than the base class's
     * read-and-discard loop.
     *
     * <p>The distinction matters for termination, not just for speed. {@link Reader#skip(long)} is implemented
     * as a loop that calls {@code read(..)} and stops only at end of input, so it spins forever on a reader
     * whose {@code read} returns {@code 0} - the very case the caller's read loop exists to survive. Only
     * readers that override {@code skip} with index arithmetic may be handed the full count.
     * {@link java.io.BufferedReader} is on the list for a different reason: its {@code skip} is a pointer move
     * within the buffer plus a refill, and the refill ({@code fill()}) itself loops while the underlying
     * {@code read} returns {@code 0} - so on such a source it would spin too, but so would every other
     * {@code BufferedReader} operation this class relies on, {@code readLine()} included. A source that answers
     * {@code 0} indefinitely is unusable through a {@code BufferedReader} either way.
     *
     * @param reader the reader to classify.
     * @return {@code true} if its {@code skip} may be used directly.
     */
    private static boolean skipsWithoutAReadLoop(final Reader reader) {
        // java.io.BufferedReader covers this library's own BufferedReader, which extends it.
        return reader instanceof java.io.BufferedReader || reader instanceof StringReader || reader instanceof java.io.CharArrayReader;
    }

    /**
     * Skips over and discards a specified number of bytes from the input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.bin")) {
     *     IOUtil.skipFully(is, 1024);
     *     byte[] data = IOUtil.readAllBytes(is);
     * }
     * }</pre>
     *
     * @param input       the input stream to be skipped, must not be {@code null}.
     * @param bytesToSkip the number of bytes to be skipped.
     * @throws IllegalArgumentException if {@code input} is {@code null}, or if {@code bytesToSkip} is negative.
     * @throws IOException if skipping or reading from {@code input} to advance its position fails
     * @throws EOFException if the stream ends before {@code bytesToSkip} bytes have been skipped. Whatever was
     *         skipped before that stays skipped - the stream is not rewound. {@code EOFException} is an
     *         {@code IOException}, and is the same type {@code org.apache.commons.io.IOUtils.skipFully} raises
     *         for this case, so it can be caught on its own to tell a short input apart from a read failure.
     */
    public static void skipFully(final InputStream input, final long bytesToSkip) throws IllegalArgumentException, IOException {
        final long skipped = skip(input, bytesToSkip);

        if (skipped != bytesToSkip) {
            throw new EOFException("Bytes to skip: " + bytesToSkip + ", actual: " + skipped);
        }
    }

    /**
     * Skips over and discards a specified number of characters from the input reader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("data.txt")) {
     *     IOUtil.skipFully(reader, 1024);
     *     char[] data = IOUtil.readAllChars(reader);
     * }
     * }</pre>
     *
     * @param input       the {@code Reader} from which characters are to be skipped, must not be {@code null}.
     * @param charsToSkip the number of characters to be skipped.
     * @throws IllegalArgumentException if {@code input} is {@code null}, or if {@code charsToSkip} is negative.
     * @throws IOException if skipping or reading from {@code input} to advance its position fails
     * @throws EOFException if the reader ends before {@code charsToSkip} characters have been skipped. Whatever
     *         was skipped before that stays skipped - the reader is not rewound. {@code EOFException} is an
     *         {@code IOException}, and is the same type {@code org.apache.commons.io.IOUtils.skipFully} raises
     *         for this case, so it can be caught on its own to tell a short input apart from a read failure.
     */
    public static void skipFully(final Reader input, final long charsToSkip) throws IllegalArgumentException, IOException {
        final long skipped = skip(input, charsToSkip);

        if (skipped != charsToSkip) {
            throw new EOFException("Chars to skip: " + charsToSkip + ", actual: " + skipped);
        }
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Maps a file into memory, creating a MappedByteBuffer that represents the file's content.
     *
     * <p>Note: this method uses only the public {@code FileChannel.map(...)} API and requires no special
     * JVM flags. Flags such as {@code --add-opens java.base/java.nio=ALL-UNNAMED} and
     * {@code --add-opens java.base/sun.nio.ch=ALL-UNNAMED} are only needed if the caller wants to forcibly
     * unmap the returned buffer via reflection into JDK internals (which this method does not do).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("large_data.bin");
     * MappedByteBuffer buffer = IOUtil.map(file);
     * byte data = buffer.get();
     * }</pre>
     *
     * <p>The mapped region cannot exceed {@link Integer#MAX_VALUE} bytes, which here means the whole file must fit.
     *
     * @param file the file to be mapped into memory, must not be {@code null} and must exist.
     * @return a MappedByteBuffer that represents the content of the file.
     * @throws IllegalArgumentException if the provided file is {@code null}, or is a directory.
     * @throws UncheckedIOException if the file does not exist (wrapping a {@link FileNotFoundException}), or if
     *         another I/O error occurs during the operation.
     * @see #map(File, MapMode)
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file) throws IllegalArgumentException, UncheckedIOException {
        return map(file, MapMode.READ_ONLY);
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Fully maps a file into memory as per
     * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
     * using the requested {@link MapMode}.
     *
     * <p>Files are mapped from offset 0 to its length.
     *
     * <p>The mapped region cannot exceed {@link Integer#MAX_VALUE} bytes, which here means the whole file must fit.
     *
     * <p>Note: this method uses only the public {@code FileChannel.map(...)} API and requires no special
     * JVM flags. Flags such as {@code --add-opens java.base/java.nio=ALL-UNNAMED} and
     * {@code --add-opens java.base/sun.nio.ch=ALL-UNNAMED} are only needed if the caller wants to forcibly
     * unmap the returned buffer via reflection into JDK internals (which this method does not do).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * MappedByteBuffer buffer = IOUtil.map(file, MapMode.READ_ONLY);
     * // Read from memory-mapped file
     * }</pre>
     *
     * <p>Unlike {@link #map(File, MapMode, long, long)}, this overload never creates the file: it maps the file's
     * <i>current</i> length, so there would be nothing to map. A missing file is reported as a
     * {@link FileNotFoundException} wrapped in {@link UncheckedIOException}, in every mode.
     *
     * @param file the file to map, must not be {@code null} and must exist.
     * @param mode the mode to use when mapping {@code file}.
     * @return a buffer reflecting {@code file}.
     * @throws IllegalArgumentException if the file or mode is {@code null}, or if the file is a directory.
     * @throws UncheckedIOException if the file does not exist (wrapping a {@link FileNotFoundException}), or if
     *         another I/O error occurs.
     * @see #map(File, MapMode, long, long)
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file, final MapMode mode) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(mode, cs.mode);

        if (!file.exists()) {
            throw new UncheckedIOException(new FileNotFoundException("'" + describe(file) + "' does not exist"));
        }

        return map(file, mode, 0, file.length());
    }

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     *
     * Maps a file into memory as per
     * {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)} using the requested {@link MapMode}.
     *
     * <p>The file is mapped from {@code offset} for {@code count} bytes.
     *
     * <p>If the mode is {@link MapMode#READ_WRITE} or {@link MapMode#PRIVATE} and the file does not exist,
     * it will be created - along with any missing parent directories, as every other file-creating method in this
     * class does - and extended to {@code offset + count} bytes.
     * Thus, this method is useful for creating memory mapped files which do not yet exist.
     * ({@code PRIVATE} is copy-on-write and still needs write access, so it opens the file exactly as
     * {@code READ_WRITE} does; the copy-on-write applies to the mapping, not to the file.)
     *
     * <p>A single mapped region cannot exceed {@link Integer#MAX_VALUE} bytes, so {@code count} is bounded by that;
     * the file itself may be larger.
     *
     * <p>With {@link MapMode#READ_ONLY} the region must already exist: an {@code offset + count} that runs past
     * the end of the file is an {@code UncheckedIOException}, because extending the file is the one thing a
     * read-only mapping cannot do. The message comes from the platform and speaks of writing
     * ({@code "Channel not open for writing - cannot extend file to required size"}), which is the reason
     * rather than the request. Map {@code Math.min(count, file.length() - offset)} bytes, or ask for
     * {@code READ_WRITE}, when the region may run short.
     *
     * <p>Note: this method uses only the public {@code FileChannel.map(...)} API and requires no special
     * JVM flags. Flags such as {@code --add-opens java.base/java.nio=ALL-UNNAMED} and
     * {@code --add-opens java.base/sun.nio.ch=ALL-UNNAMED} are only needed if the caller wants to forcibly
     * unmap the returned buffer via reflection into JDK internals (which this method does not do).
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
     * @param file   the file to map. With {@link MapMode#READ_ONLY} it must exist; with
     *               {@link MapMode#READ_WRITE} or {@link MapMode#PRIVATE} - both of which need write
     *               access - it is created, along with any missing parent directories, if it does not.
     * @param mode   the mode to use when mapping {@code file}.
     * @param offset the offset within the file at which the mapped region is to start; must be non-negative.
     * @param count  the size of the region to be mapped; must be non-negative and no greater than
     *               {@link Integer#MAX_VALUE}.
     * @return a buffer reflecting {@code file}.
     * @throws IllegalArgumentException if the preconditions on the parameters do not hold - including a
     *         {@code count} greater than {@link Integer#MAX_VALUE}, which {@link FileChannel#map} rejects, or
     *         a {@code file} that exists but is a directory.
     * @throws UncheckedIOException if the file does not exist and cannot be created for the requested mode
     *         (wrapping a {@link FileNotFoundException}), if a missing parent directory cannot be created, or if
     *         another I/O error occurs.
     * @see #map(File, MapMode)
     * @see FileChannel#map(MapMode, long, long)
     */
    public static MappedByteBuffer map(final File file, final MapMode mode, final long offset, final long count)
            throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(mode, cs.mode);
        N.checkArgNotNegative(offset, cs.offset);
        N.checkArgNotNegative(count, cs.count);

        // A directory is a path of the wrong kind, which the class contract reports as IllegalArgumentException -
        // not an I/O failure. Left to RandomAccessFile it surfaces as an UncheckedIOException whose message is
        // platform-dependent ("Access is denied" on Windows, "Is a directory" on Unix) for what is simply a bad
        // argument. Checking here covers all three map(..) overloads, since they all funnel through this one.
        if (file.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(file) + "' is a directory, not a file");
        }

        // RandomAccessFile("rw") creates the file but not its directory, so the documented "created if it does not
        // exist" failed with FileNotFoundException for a file in a directory that did not exist yet - the one
        // file-creating path in the class that did not mkdirs. READ_ONLY never creates anything, so it is left
        // alone; a missing file there is reported by the open below, as before.
        if (mode != MapMode.READ_ONLY && !createParentDirectories(file)) {
            throw new UncheckedIOException(new IOException("Failed to create parent directory: " + describe(getParentFile(file))));
        }

        // The mapping outlives the channel and the file handle, so both are closed before returning.
        try (RandomAccessFile raf = new RandomAccessFile(file, mode == MapMode.READ_ONLY ? "r" : "rw")) {
            return raf.getChannel().map(mode, offset, count);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
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
     * <li>fold out ./
     * <li>fold out ../ when possible
     * <li>collapse multiple slashes
     * <li>delete trailing slashes (unless the path is just "/")
     * <li>backslashes are treated as separators and normalized to {@code /}
     * <li>a Windows drive prefix such as {@code C:/} is kept as the root and cannot be ascended above
     * <li>a UNC prefix such as {@code //host/share} keeps both leading slashes and is kept as the root
     * </ul>
     *
     * <p>These heuristics do not always match the behavior of the filesystem. In
     * particular, consider the path {@code a/../b}, which {@code simplifyPath}
     * will change to {@code b}. If {@code a} is a symlink to {@code x}, {@code
     * a/../b} may refer to a sibling of {@code x}, rather than the sibling of
     * {@code a} referred to by {@code b}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String simplified = IOUtil.simplifyPath("/a/./b/./c/");   // returns "/a/b/c"
     * String simplified2 = IOUtil.simplifyPath("a/b/./c");      // returns "a/b/c"
     * String simplified3 = IOUtil.simplifyPath("a/b/../c");     // returns "a/c"
     * String simplified4 = IOUtil.simplifyPath("a\\b\\c");      // returns "a/b/c"
     * String simplified5 = IOUtil.simplifyPath("");             // returns "."
     * String simplified6 = IOUtil.simplifyPath("//host/share/./x");   // returns "//host/share/x"
     * String simplified7 = IOUtil.simplifyPath("C:/a/../../b");       // returns "C:/b"
     * }</pre>
     *
     * @param pathname the file path to simplify; {@code null} or empty yields {@code "."}.
     * @return the simplified path string with redundant elements removed; never {@code null}.
     * @see java.nio.file.Path#normalize()
     * @see FilenameUtil#normalize(String)
     * @see FilenameUtil#normalize(String, boolean)
     */
    public static String simplifyPath(String pathname) {
        if (Strings.isEmpty(pathname)) {
            return ".";
        }

        pathname = pathname.replace('\\', '/');
        final boolean windowsAbsolutePath = pathname.length() >= 3 && isDriveLetter(pathname.charAt(0)) && pathname.charAt(1) == ':'
                && pathname.charAt(2) == '/';
        // A UNC path ("//host/share/..." or the "\\host\share\..." form already normalized above) keeps its
        // two-slash prefix: collapsing it to a single slash would silently turn a valid network path into an
        // invalid local one. Exactly two leading slashes introduce a UNC root; three or more are just redundant
        // separators on an ordinary absolute path, matching how the platform resolves them.
        final boolean uncPath = !windowsAbsolutePath && pathname.length() > 2 && pathname.charAt(0) == '/' && pathname.charAt(1) == '/'
                && pathname.charAt(2) != '/';
        final boolean absolutePath = pathname.charAt(0) == '/' || windowsAbsolutePath;
        // The drive letter of "C:/..." and the "host/share" pair of "//host/share/..." are roots: '..' can never
        // ascend past them.
        final int rootComponentCount = windowsAbsolutePath ? 1 : (uncPath ? 2 : 0);

        // split the path apart
        final String[] components = pathSplitter.splitToArray(pathname);
        final List<String> path = new ArrayList<>();

        // resolve ., .., and //
        for (final String component : components) {
            if (component.isEmpty() || component.equals(".")) {
                //NOSONAR
            } else if (component.equals("..")) {
                if (path.size() > rootComponentCount && !path.get(path.size() - 1).equals("..")) {
                    path.remove(path.size() - 1);
                } else if (absolutePath) {
                    // An absolute path cannot ascend above its root. In particular, keep a Windows drive prefix
                    // ("C:/") or a UNC "//host/share" prefix instead of treating them as removable directory names.
                } else {
                    path.add("..");
                }
            } else {
                path.add(component);
            }
        }

        // put it back together
        String result = Strings.join(path, "/");

        if (uncPath) {
            result = "//" + result;
        } else if (pathname.charAt(0) == '/') {
            result = "/" + result;
        } else if (windowsAbsolutePath && result.length() == 2) {
            result += "/";
        }

        if (result.isEmpty()) {
            result = ".";
        }

        // Note: no post-pass is needed to strip a leading "/..". The loop above never appends ".." once the path
        // is absolute, so the joined result can never begin with one.

        return result;
    }

    /**
     * Whether {@code ch} can introduce a Windows drive specifier such as {@code "C:"}.
     *
     * <p>ASCII only, deliberately: {@link Character#isLetter(char)} accepts every letter in Unicode, so a
     * {@code "α:/a"} was read as a drive-rooted absolute path that {@code ".."} could not ascend, while
     * the neighbouring {@code "1:/a"} was correctly read as an ordinary relative one. Windows drives are
     * {@code A}-{@code Z} and nothing else.
     *
     * @param ch the first character of a candidate drive specifier.
     * @return {@code true} if it is an ASCII letter.
     */
    private static boolean isDriveLetter(final char ch) {
        return (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
    }

    /**
     * Returns the file extension of the specified file. Empty string is returned if the file has no extension.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("document.pdf");
     * String ext = IOUtil.getFileExtension(file);  // returns "pdf"
     * }</pre>
     *
     * @param file the file whose extension is to be retrieved.
     * @return the file extension, or {@code null} if the file is {@code null}.
     * @throws IllegalArgumentException if the file's name contains a {@code null} byte.
     * @see FilenameUtil#getExtension(String)
     */
    @MayReturnNull
    public static String getFileExtension(final File file) throws IllegalArgumentException {
        if (file == null) {
            return null;
        }

        return FilenameUtil.getExtension(file.getName());
    }

    /**
     * Returns the file extension of the specified file name. Empty string is returned if the file has no extension.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String ext = IOUtil.getFileExtension("document.pdf");   // returns "pdf"
     * String noExt = IOUtil.getFileExtension("README");       // returns ""
     * }</pre>
     *
     * @param fileName the name of the file whose extension is to be retrieved.
     * @return the file extension, or {@code null} if the specified file name is {@code null}.
     * @throws IllegalArgumentException if the file name contains a {@code null} byte.
     * @see FilenameUtil#getExtension(String)
     */
    @MayReturnNull
    public static String getFileExtension(final String fileName) throws IllegalArgumentException {
        return FilenameUtil.getExtension(fileName);
    }

    /**
     * Returns the file name without its extension.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("document.pdf");
     * String name = IOUtil.getNameWithoutExtension(file);  // returns "document"
     * }</pre>
     *
     * @param file the file whose name without extension is to be retrieved.
     * @return the file name without extension, or {@code null} if the file is {@code null}.
     * @throws IllegalArgumentException if the file's name contains a {@code null} byte.
     * @see FilenameUtil#removeExtension(String)
     */
    @MayReturnNull
    public static String getNameWithoutExtension(final File file) throws IllegalArgumentException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOUtil.getNameWithoutExtension("foo.txt");       // returns "foo"
     * IOUtil.getNameWithoutExtension("a\\b\\c.jpg");   // returns "a\\b\\c"
     * IOUtil.getNameWithoutExtension("a\\b\\c");       // returns "a\\b\\c"
     * IOUtil.getNameWithoutExtension("a.b\\c");        // returns "a.b\\c"
     * IOUtil.getNameWithoutExtension((String) null);   // returns null (a bare null is ambiguous with the File overload)
     * }</pre>
     *
     * @param fileName the filename to query, {@code null} returns {@code null}.
     * @return the filename minus the extension, or {@code null} if the input is {@code null}.
     * @throws IllegalArgumentException if the file name contains a {@code null} byte.
     * @see FilenameUtil#removeExtension(String)
     */
    @MayReturnNull
    public static String getNameWithoutExtension(final String fileName) throws IllegalArgumentException {
        return FilenameUtil.removeExtension(fileName);
    }

    /**
     * Creates a new AppendableWriter instance that wraps the provided Appendable object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * Writer writer = IOUtil.newAppendableWriter(sb);
     * writer.write("Hello");
     * // sb now contains "Hello"
     * }</pre>
     *
     * @param appendable the Appendable object to be wrapped by the AppendableWriter, must not be {@code null}.
     * @return a new instance of AppendableWriter that wraps the provided Appendable.
     * @throws IllegalArgumentException if the appendable is {@code null}.
     */
    public static AppendableWriter newAppendableWriter(final Appendable appendable) throws IllegalArgumentException {
        return new AppendableWriter(appendable);
    }

    /**
     * Creates a new {@link com.landawn.abacus.util.StringWriter} instance.
     *
     * <p><b>Not {@link java.io.StringWriter}.</b> This class's own {@code StringWriter} extends
     * {@link AppendableWriter}, not {@code java.io.StringWriter}, so the two are unrelated types that merely
     * share a simple name. Write the type out - or import it explicitly - in any file that also has the
     * {@code java.io} name in scope, or the assignment will not compile.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.landawn.abacus.util.StringWriter writer = IOUtil.newStringWriter();
     * writer.write("Hello, World!");
     * String result = writer.toString();   // returns "Hello, World!"
     * }</pre>
     *
     * @return a new instance of {@link com.landawn.abacus.util.StringWriter}.
     * @see #newStringWriter(int)
     * @see #newStringWriter(StringBuilder)
     */
    public static StringWriter newStringWriter() {
        return new StringWriter();
    }

    /**
     * Creates a new StringWriter instance with the specified initial size.
     *
     * <p>The returned writer is {@link com.landawn.abacus.util.StringWriter}, not {@link java.io.StringWriter};
     * see {@link #newStringWriter()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.landawn.abacus.util.StringWriter writer = IOUtil.newStringWriter(1024);
     * writer.write("Content");
     * String result = writer.toString();   // returns "Content"
     * }</pre>
     *
     * @param initialSize the initial size of the buffer; must not be negative.
     * @return a new instance of {@link com.landawn.abacus.util.StringWriter} with the specified initial size.
     * @throws IllegalArgumentException if {@code initialSize} is negative.
     * @see #newStringWriter()
     */
    public static StringWriter newStringWriter(final int initialSize) throws IllegalArgumentException {
        N.checkArgNotNegative(initialSize, cs.initialSize);

        return new StringWriter(initialSize);
    }

    /**
     * Wraps the specified {@code StringBuilder} in a {@code StringWriter}. The builder is used directly as the
     * writer's buffer, so everything written to the returned writer is appended to {@code sb} and any content
     * {@code sb} already holds is kept.
     *
     * <p>The returned writer is {@link com.landawn.abacus.util.StringWriter}, not {@link java.io.StringWriter}
     * (which has no such constructor); see {@link #newStringWriter()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder("Initial");
     * com.landawn.abacus.util.StringWriter writer = IOUtil.newStringWriter(sb);
     * writer.write(" content");
     * // sb now contains "Initial content"
     * }</pre>
     *
     * @param sb the StringBuilder to wrap, must not be {@code null}.
     * @return a new {@link com.landawn.abacus.util.StringWriter} that appends to the specified string builder.
     * @throws IllegalArgumentException if {@code sb} is {@code null}.
     * @see #newStringWriter()
     */
    public static StringWriter newStringWriter(final StringBuilder sb) throws IllegalArgumentException {
        N.checkArgNotNull(sb, cs.sb);

        return new StringWriter(sb);
    }

    /**
     * Creates a new {@link com.landawn.abacus.util.ByteArrayOutputStream} instance.
     *
     * <p><b>Not {@link java.io.ByteArrayOutputStream}.</b> This class's own {@code ByteArrayOutputStream}
     * extends {@link OutputStream} directly, not {@code java.io.ByteArrayOutputStream}, so the two are
     * unrelated types that merely share a simple name. Write the type out - or import it explicitly - in any
     * file that also has the {@code java.io} name in scope, or the assignment will not compile.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.landawn.abacus.util.ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream();
     * baos.write(65);  // Write byte value 65 ('A')
     * byte[] result = baos.toByteArray();   // returns {(byte) 65}
     * }</pre>
     *
     * @return a new instance of {@link com.landawn.abacus.util.ByteArrayOutputStream}.
     * @see #newByteArrayOutputStream(int)
     */
    public static ByteArrayOutputStream newByteArrayOutputStream() {
        return new ByteArrayOutputStream();
    }

    /**
     * Creates a new ByteArrayOutputStream instance with the specified initial capacity.
     *
     * <p>The returned stream is {@link com.landawn.abacus.util.ByteArrayOutputStream}, not
     * {@link java.io.ByteArrayOutputStream}; see {@link #newByteArrayOutputStream()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * com.landawn.abacus.util.ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream(1024);
     * baos.write("Hello".getBytes(StandardCharsets.UTF_8));
     * byte[] result = baos.toByteArray();   // returns the 5 bytes of "Hello"
     * }</pre>
     *
     * @param initCapacity the initial capacity of the ByteArrayOutputStream; must not be negative.
     * @return a new instance of {@link com.landawn.abacus.util.ByteArrayOutputStream} with the specified initial capacity.
     * @throws IllegalArgumentException if {@code initCapacity} is negative.
     * @see #newByteArrayOutputStream()
     */
    public static ByteArrayOutputStream newByteArrayOutputStream(final int initCapacity) throws IllegalArgumentException {
        return new ByteArrayOutputStream(initCapacity);
    }

    /**
     * Creates a new FileInputStream instance for the specified file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * try (FileInputStream fis = IOUtil.newFileInputStream(file)) {
     *     int data = fis.read();
     * }
     * }</pre>
     *
     * @param file the file to be opened for reading.
     * @return a new FileInputStream instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for reading fails
     * @see FileInputStream#FileInputStream(File)
     */
    public static FileInputStream newFileInputStream(final File file) throws IllegalArgumentException, UncheckedIOException {
        try {
            return openFileInputStream(file);
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileInputStream instance for the specified file name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileInputStream fis = IOUtil.newFileInputStream("data.bin")) {
     *     byte[] buffer = new byte[1024];
     *     int bytesRead = fis.read(buffer);
     * }
     * }</pre>
     *
     * @param name the name of the file to be opened for reading.
     * @return a new FileInputStream instance.
     * @throws IllegalArgumentException if {@code name} is {@code null}, or names a directory rather than a file.
     * @throws UncheckedIOException if opening {@code name} for reading fails
     * @see #newFileInputStream(File)
     * @see FileInputStream#FileInputStream(String)
     */
    public static FileInputStream newFileInputStream(final String name) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(name, cs.name);

        try {
            return openFileInputStream(new File(name));
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileOutputStream instance for the specified file, truncating it if it already exists.
     * The file, along with any missing parent directories, is created first if it does not exist.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("output.bin");
     * try (FileOutputStream fos = IOUtil.newFileOutputStream(file)) {
     *     fos.write("Hello".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param file the file to be opened for writing. It is created, along with any missing parent directories, if it does not exist.
     * @return a new FileOutputStream instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see FileOutputStream#FileOutputStream(File)
     */
    public static FileOutputStream newFileOutputStream(final File file) throws IllegalArgumentException, UncheckedIOException {
        try {
            return openFileOutputStream(file);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileOutputStream instance for the specified file.
     * The file, along with any missing parent directories, is created first if it does not exist.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("output.txt");
     * try (FileOutputStream fos = IOUtil.newFileOutputStream(file, true)) {
     *     fos.write("Appended content".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param file   the file to be opened for writing. It is created, along with any missing parent directories, if it does not exist.
     * @param append {@code true} to append to the existing content instead of truncating the file.
     * @return a new FileOutputStream instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see FileOutputStream#FileOutputStream(File, boolean)
     */
    public static FileOutputStream newFileOutputStream(final File file, final boolean append) throws IllegalArgumentException, UncheckedIOException {
        try {
            return openFileOutputStream(file, append);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileOutputStream instance for the specified file name, truncating the file if it already exists.
     * The file, along with any missing parent directories, is created first if it does not exist.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileOutputStream fos = IOUtil.newFileOutputStream("output.bin")) {
     *     fos.write(new byte[]{1, 2, 3});
     * }
     * }</pre>
     *
     * @param name the name of the file to be opened for writing. It is created, along with any missing parent directories, if it does not exist.
     * @return a new FileOutputStream instance.
     * @throws IllegalArgumentException if {@code name} is {@code null}, or names a directory rather than a file.
     * @throws UncheckedIOException if opening {@code name} for writing fails
     * @see #newFileOutputStream(File)
     * @see #newFileOutputStream(String, boolean)
     * @see FileOutputStream#FileOutputStream(String)
     */
    public static FileOutputStream newFileOutputStream(final String name) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(name, cs.name);

        return newFileOutputStream(new File(name));
    }

    /**
     * Creates a new FileOutputStream instance for the specified file name.
     * The file, along with any missing parent directories, is created first if it does not exist.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileOutputStream fos = IOUtil.newFileOutputStream("output.txt", true)) {
     *     fos.write("Appended content".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param name   the name of the file to be opened for writing. It is created, along with any missing parent directories, if it does not exist.
     * @param append {@code true} to append to the existing content instead of truncating the file.
     * @return a new FileOutputStream instance.
     * @throws IllegalArgumentException if {@code name} is {@code null}, or names a directory rather than a file.
     * @throws UncheckedIOException if opening {@code name} for writing fails
     * @see #newFileOutputStream(File, boolean)
     * @see FileOutputStream#FileOutputStream(String, boolean)
     */
    public static FileOutputStream newFileOutputStream(final String name, final boolean append) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(name, cs.name);

        return newFileOutputStream(new File(name), append);
    }

    /**
     * Creates a new FileReader instance for the specified file and the default Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text.txt");
     * try (FileReader reader = IOUtil.newFileReader(file)) {
     *     int ch = reader.read();
     * }
     * }</pre>
     *
     * @param file the file to be opened for reading.
     * @return a new FileReader instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for reading fails
     * @see FileReader#FileReader(File, Charset)
     */
    public static FileReader newFileReader(final File file) throws IllegalArgumentException, UncheckedIOException {
        try {
            return openFileReader(file, DEFAULT_CHARSET);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileReader instance for the specified file and charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("text.txt");
     * try (FileReader reader = IOUtil.newFileReader(file, StandardCharsets.UTF_8)) {
     *     char[] buffer = new char[1024];
     *     int charsRead = reader.read(buffer);
     * }
     * }</pre>
     *
     * @param file    the file to be opened for reading.
     * @param charset the Charset to be used for creating the FileReader; {@code null} uses the default charset (UTF-8).
     * @return a new FileReader instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for reading fails
     * @see FileReader#FileReader(File, Charset)
     */
    public static FileReader newFileReader(final File file, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        try {
            return openFileReader(file, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileWriter instance for the specified file and the default Charset, truncating the file if it already exists.
     * The file, along with any missing parent directories, is created first if it does not exist.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("output.txt");
     * try (FileWriter writer = IOUtil.newFileWriter(file)) {
     *     writer.write("Hello, World!");
     * }
     * }</pre>
     *
     * @param file the file to be opened for writing. It is created, along with any missing parent directories, if it does not exist.
     * @return a new FileWriter instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see FileWriter#FileWriter(File, Charset)
     */
    public static FileWriter newFileWriter(final File file) throws IllegalArgumentException, UncheckedIOException {
        try {
            return openFileWriter(file, DEFAULT_CHARSET);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileWriter instance for the specified file and charset, truncating the file if it already exists.
     * The file, along with any missing parent directories, is created first if it does not exist.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("output.txt");
     * try (FileWriter writer = IOUtil.newFileWriter(file, StandardCharsets.UTF_8)) {
     *     writer.write("Hello, UTF-8!");
     * }
     * }</pre>
     *
     * @param file    the file to be opened for writing. It is created, along with any missing parent directories, if it does not exist.
     * @param charset the Charset to be used for creating the FileWriter; {@code null} uses the default charset (UTF-8).
     * @return a new FileWriter instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see FileWriter#FileWriter(File, Charset)
     */
    public static FileWriter newFileWriter(final File file, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        try {
            return openFileWriter(file, charset);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new FileWriter instance for the specified file and charset.
     * The file, along with any missing parent directories, is created first if it does not exist.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("log.txt");
     * try (FileWriter writer = IOUtil.newFileWriter(file, StandardCharsets.UTF_8, true)) {
     *     writer.write("New log entry\n");
     * }
     * }</pre>
     *
     * @param file    the file to be opened for writing. It is created, along with any missing parent directories, if it does not exist.
     * @param charset the Charset to be used for creating the FileWriter; {@code null} uses the default charset (UTF-8).
     * @param append  {@code true} to append to the existing content instead of truncating the file.
     * @return a new FileWriter instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see FileWriter#FileWriter(File, Charset, boolean)
     */
    public static FileWriter newFileWriter(final File file, final Charset charset, final boolean append) throws IllegalArgumentException, UncheckedIOException {
        try {
            return openFileWriter(file, charset, append);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Rethrows a failed file open as the exception this class's contract calls for: a path that exists but is
     * a directory is a wrong-<i>kind</i> argument and is reported as {@link IllegalArgumentException}, while
     * everything else keeps the failure the platform raised.
     *
     * <p>Without this the platform's own message is what reaches the caller, and it names the wrong problem:
     * {@code FileNotFoundException: <path> (Access is denied)} on Windows, {@code (Is a directory)} on Unix.
     * The first points at permissions and neither points at the argument.
     *
     * <p>Classified <b>here, in the failure path</b>, rather than by testing the argument up front: opening a
     * directory always fails, so the question only ever has to be asked once an open has already failed.
     * Measured on an idle Windows box, one {@link File#isDirectory()} stat costs ~10us against ~34us to read a
     * small file end to end, so asking eagerly would add roughly a third to every open in this class.
     *
     * @param <E>     the failure type, preserved so a caller declaring only {@code FileNotFoundException} still compiles.
     * @param file    the file whose open failed.
     * @param failure the failure the platform raised.
     * @return {@code failure}, for the caller to rethrow.
     * @throws IllegalArgumentException if {@code file} is a directory.
     */
    private static <E extends IOException> E classifyFailedOpen(final File file, final E failure) throws IllegalArgumentException {
        if (file.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(file) + "' is a directory, not a file", failure);
        }

        return failure;
    }

    /**
     * Checked counterpart of {@link #newFileInputStream(File)} for methods that declare {@code IOException}.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or a failed open identifies it as a directory
     * @throws FileNotFoundException if {@code file} does not exist or cannot be opened for reading, and is not identified as a directory
     */
    private static FileInputStream openFileInputStream(final File file) throws IllegalArgumentException, FileNotFoundException {
        N.checkArgNotNull(file, cs.file);

        try {
            return new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            throw classifyFailedOpen(file, e);
        }
    }

    /**
     * Checked counterpart of {@link #newFileOutputStream(File)} for methods that declare {@code IOException}.
     */
    private static FileOutputStream openFileOutputStream(final File file) throws IOException {
        return openFileOutputStream(file, false);
    }

    /**
     * Checked counterpart of {@link #newFileOutputStream(File, boolean)} for methods that declare {@code IOException}.
     */
    private static FileOutputStream openFileOutputStream(final File file, final boolean append) throws IOException {
        // Only the open is classified: createNewFileIfNotExists reports a parent directory it could not create,
        // which is a real environment failure rather than a wrong-kind argument, and must pass through as-is.
        createNewFileIfNotExists(file);

        try {
            return new FileOutputStream(file, append);
        } catch (final FileNotFoundException e) {
            throw classifyFailedOpen(file, e);
        }
    }

    /**
     * Checked counterpart of {@link #newFileReader(File, Charset)} for methods that declare {@code IOException}.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or a failed open identifies it as a directory
     * @throws IOException if {@code file} does not exist or cannot be opened for reading, and is not identified as a directory
     */
    private static FileReader openFileReader(final File file, final Charset charset) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);

        try {
            return new FileReader(file, checkCharset(charset));
        } catch (final IOException e) {
            throw classifyFailedOpen(file, e);
        }
    }

    /**
     * Checked counterpart of {@link #newFileWriter(File, Charset)} for methods that declare {@code IOException}.
     */
    private static FileWriter openFileWriter(final File file, final Charset charset) throws IOException {
        return openFileWriter(file, charset, false);
    }

    /**
     * Checked counterpart of {@link #newFileWriter(File, Charset, boolean)} for methods that declare {@code IOException}.
     */
    private static FileWriter openFileWriter(final File file, final Charset charset, final boolean append) throws IOException {
        // See openFileOutputStream(File, boolean): only the open itself is classified.
        createNewFileIfNotExists(file);

        try {
            return new FileWriter(file, checkCharset(charset), append);
        } catch (final IOException e) {
            throw classifyFailedOpen(file, e);
        }
    }

    /**
     * Creates a new InputStreamReader instance for the specified InputStream and the default Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.txt");
     *      InputStreamReader reader = IOUtil.newInputStreamReader(is)) {
     *     int ch = reader.read();
     * }
     * }</pre>
     *
     * @param is the InputStream to be read. It must not be {@code null}.
     * @return a new InputStreamReader instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @see InputStreamReader#InputStreamReader(InputStream, Charset)
     */
    public static InputStreamReader newInputStreamReader(final InputStream is) throws IllegalArgumentException {
        N.checkArgNotNull(is, cs.is);

        return new InputStreamReader(is, DEFAULT_CHARSET); // NOSONAR
    }

    /**
     * Creates a new InputStreamReader instance for the specified InputStream and Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.txt");
     *      InputStreamReader reader = IOUtil.newInputStreamReader(is, StandardCharsets.UTF_8)) {
     *     char[] buffer = new char[1024];
     *     reader.read(buffer);
     * }
     * }</pre>
     *
     * @param is      the InputStream to be read. It must not be {@code null}.
     * @param charset the Charset to be used for creating the InputStreamReader; {@code null} uses the default charset (UTF-8).
     * @return a new InputStreamReader instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @see InputStreamReader#InputStreamReader(InputStream, Charset)
     */
    public static InputStreamReader newInputStreamReader(final InputStream is, final Charset charset) throws IllegalArgumentException {
        N.checkArgNotNull(is, cs.is);

        return new InputStreamReader(is, checkCharset(charset)); // NOSONAR
    }

    /**
     * Creates a new OutputStreamWriter instance for the specified OutputStream and the default Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("output.txt");
     *      OutputStreamWriter writer = IOUtil.newOutputStreamWriter(os)) {
     *     writer.write("Hello!");
     * }
     * }</pre>
     *
     * @param os the OutputStream to be written to. It must not be {@code null}.
     * @return a new OutputStreamWriter instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     * @see OutputStreamWriter#OutputStreamWriter(OutputStream, Charset)
     */
    public static OutputStreamWriter newOutputStreamWriter(final OutputStream os) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new OutputStreamWriter(os, DEFAULT_CHARSET); // NOSONAR
    }

    /**
     * Creates a new OutputStreamWriter instance for the specified OutputStream and Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("output.txt");
     *      OutputStreamWriter writer = IOUtil.newOutputStreamWriter(os, StandardCharsets.UTF_8)) {
     *     writer.write("UTF-8 content");
     * }
     * }</pre>
     *
     * @param os      the OutputStream to be written to. It must not be {@code null}.
     * @param charset the Charset to be used for creating the OutputStreamWriter; {@code null} uses the default charset (UTF-8).
     * @return a new OutputStreamWriter instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     * @see OutputStreamWriter#OutputStreamWriter(OutputStream, Charset)
     */
    public static OutputStreamWriter newOutputStreamWriter(final OutputStream os, final Charset charset) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new OutputStreamWriter(os, checkCharset(charset));
    }

    /**
     * Creates a new BufferedInputStream instance that wraps the specified InputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.bin");
     *      BufferedInputStream bis = IOUtil.newBufferedInputStream(is)) {
     *     int data = bis.read();
     * }
     * }</pre>
     *
     * @param is the InputStream to be wrapped. It must not be {@code null}.
     * @return a new BufferedInputStream instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @see BufferedInputStream#BufferedInputStream(InputStream)
     */
    public static BufferedInputStream newBufferedInputStream(final InputStream is) throws IllegalArgumentException {
        N.checkArgNotNull(is, cs.is);

        return new BufferedInputStream(is);
    }

    /**
     * Creates a new BufferedReader instance for the specified file path.
     *
     * @param filePath the path of the file to be read.
     * @return a new BufferedReader instance.
     * @throws IllegalArgumentException if {@code filePath} is {@code null}
     * @throws UncheckedIOException if opening {@code filePath} for reading fails
     * @see #newBufferedReader(File)
     */
    static java.io.BufferedReader newBufferedReader(final String filePath) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(filePath, cs.filePath);

        return newBufferedReader(new File(filePath));
    }

    /**
     * Creates a new BufferedInputStream instance for the specified file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * try (BufferedInputStream bis = IOUtil.newBufferedInputStream(file)) {
     *     byte[] buffer = new byte[1024];
     *     bis.read(buffer);
     * }
     * }</pre>
     *
     * @param file the file to be read.
     * @return a new BufferedInputStream instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for reading fails
     * @see #newFileInputStream(File)
     * @see BufferedInputStream#BufferedInputStream(InputStream)
     */
    public static BufferedInputStream newBufferedInputStream(final File file) throws IllegalArgumentException, UncheckedIOException {
        return new BufferedInputStream(newFileInputStream(file));
    }

    /**
     * Creates a new BufferedInputStream instance for the specified file with a specified buffer size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.bin");
     * try (BufferedInputStream bis = IOUtil.newBufferedInputStream(file, 8192)) {
     *     byte[] buffer = new byte[1024];
     *     bis.read(buffer);
     * }
     * }</pre>
     *
     * @param file the file to be read.
     * @param size the size of the buffer to be used.
     * @return a new BufferedInputStream instance.
     * @throws IllegalArgumentException if {@code size} is not positive, or if {@code file} is {@code null} or is
     *         a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for reading fails
     * @see #newFileInputStream(File)
     * @see BufferedInputStream#BufferedInputStream(InputStream, int)
     */
    public static BufferedInputStream newBufferedInputStream(final File file, final int size) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgPositive(size, cs.size);

        return new BufferedInputStream(newFileInputStream(file), size);
    }

    /**
     * Creates a new BufferedOutputStream instance that wraps the specified OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("output.bin");
     *      BufferedOutputStream bos = IOUtil.newBufferedOutputStream(os)) {
     *     bos.write("Hello".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os the OutputStream to be wrapped. It must not be {@code null}.
     * @return a new BufferedOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     * @see BufferedOutputStream#BufferedOutputStream(OutputStream)
     */
    public static BufferedOutputStream newBufferedOutputStream(final OutputStream os) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new BufferedOutputStream(os);
    }

    /**
     * Creates a new BufferedOutputStream instance for the specified file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("output.bin");
     * try (BufferedOutputStream bos = IOUtil.newBufferedOutputStream(file)) {
     *     bos.write(new byte[]{1, 2, 3});
     * }
     * }</pre>
     *
     * @param file the file to be written to. It is created, along with any missing parent directories, if it does not exist; otherwise it is truncated.
     * @return a new BufferedOutputStream instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see #newFileOutputStream(File)
     * @see BufferedOutputStream#BufferedOutputStream(OutputStream)
     */
    public static BufferedOutputStream newBufferedOutputStream(final File file) throws IllegalArgumentException, UncheckedIOException {
        return new BufferedOutputStream(newFileOutputStream(file));
    }

    /**
     * Creates a new BufferedOutputStream instance for the specified file with a specified buffer size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("output.bin");
     * try (BufferedOutputStream bos = IOUtil.newBufferedOutputStream(file, 8192)) {
     *     bos.write("Data".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param file the file to be written to. It is created, along with any missing parent directories, if it does not exist; otherwise it is truncated.
     * @param size the size of the buffer to be used.
     * @return a new BufferedOutputStream instance.
     * @throws IllegalArgumentException if {@code size} is not positive, or if {@code file} is {@code null} or is
     *         a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see #newFileOutputStream(File)
     * @see BufferedOutputStream#BufferedOutputStream(OutputStream, int)
     */
    public static BufferedOutputStream newBufferedOutputStream(final File file, final int size) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgPositive(size, cs.size);

        return new BufferedOutputStream(newFileOutputStream(file), size);
    }

    /**
     * Creates a new BufferedReader instance that wraps the specified Reader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("file.txt");
     *      BufferedReader br = IOUtil.newBufferedReader(reader)) {
     *     String line = br.readLine();
     * }
     * }</pre>
     *
     * @param reader the Reader to be wrapped. It must not be {@code null}.
     * @return a new BufferedReader instance.
     * @throws IllegalArgumentException if {@code reader} is {@code null}.
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final Reader reader) throws IllegalArgumentException {
        N.checkArgNotNull(reader, cs.reader);

        return new java.io.BufferedReader(reader);
    }

    /**
     * Creates a new BufferedReader instance for the specified file with the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("file.txt");
     * try (BufferedReader br = IOUtil.newBufferedReader(file)) {
     *     String line = br.readLine();
     * }
     * }</pre>
     *
     * @param file the file to be read from.
     * @return a new BufferedReader instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for reading fails
     * @see #newFileReader(File)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final File file) throws IllegalArgumentException, UncheckedIOException {
        return new java.io.BufferedReader(newFileReader(file));
    }

    /**
     * Creates a new BufferedReader instance for the specified file with a specified charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("file.txt");
     * try (BufferedReader br = IOUtil.newBufferedReader(file, StandardCharsets.UTF_8)) {
     *     String line = br.readLine();
     * }
     * }</pre>
     *
     * @param file    the file to be read from.
     * @param charset the charset to be used; {@code null} uses the default charset (UTF-8).
     * @return a new BufferedReader instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for reading fails
     * @see #newFileReader(File, Charset)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final File file, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        return new java.io.BufferedReader(newFileReader(file, checkCharset(charset)));
    }

    /**
     * Creates a new BufferedReader instance for the specified path with the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("file.txt");
     * try (BufferedReader br = IOUtil.newBufferedReader(path)) {
     *     String line = br.readLine();
     * }
     * }</pre>
     *
     * <p>Answers exactly as {@link #newBufferedReader(File)} does - see {@link #newBufferedReader(Path, Charset)}
     * for the contract, which this class's {@code File} twin has always followed and the {@code Path} form used to
     * depart from.
     *
     * @param path the path of the file to be read from.
     * @return a new BufferedReader instance.
     * @throws IllegalArgumentException if {@code path} is {@code null}, or exists but is a directory.
     * @throws UncheckedIOException if the file does not exist (wrapping a {@link FileNotFoundException}) or cannot
     *         be opened.
     * @see #newBufferedReader(Path, Charset)
     * @see #newBufferedReader(File)
     */
    public static java.io.BufferedReader newBufferedReader(final Path path) throws IllegalArgumentException, UncheckedIOException {
        return newBufferedReader(path, DEFAULT_CHARSET);
    }

    /**
     * Creates a new BufferedReader instance for the specified path with a specified charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path path = Paths.get("file.txt");
     * try (BufferedReader br = IOUtil.newBufferedReader(path, StandardCharsets.UTF_8)) {
     *     String line = br.readLine();
     * }
     * }</pre>
     *
     * <p><b>Same answers as the {@code File} twin.</b> This is {@link #newBufferedReader(File, Charset)} for a
     * {@code Path}, and it follows the class contract the way that twin does: a path that exists but is a
     * directory is a wrong-kind argument ({@link IllegalArgumentException}), an absent one is a
     * {@link FileNotFoundException} wrapped in {@link UncheckedIOException}, and a byte sequence that is not
     * valid for the charset is replaced with {@code U+FFFD} rather than reported. It is deliberately <i>not</i>
     * {@link java.nio.file.Files#newBufferedReader(Path, Charset)}, whose reader raises
     * {@link java.nio.charset.MalformedInputException} part-way through a read, opens a directory without
     * complaint on Unix and fails only when read, and names an absent file with
     * {@link java.nio.file.NoSuchFileException} - three answers no other method in this class gives. A path on a
     * provider other than the default file system (a zip file system, say) is opened through that provider and
     * gets the same three answers.
     *
     * @param path    the path of the file to be read from.
     * @param charset the charset to be used; {@code null} uses the default charset (UTF-8).
     * @return a new BufferedReader instance.
     * @throws IllegalArgumentException if {@code path} is {@code null}, or exists but is a directory.
     * @throws UncheckedIOException if the file does not exist (wrapping a {@link FileNotFoundException}) or cannot
     *         be opened.
     * @see #newBufferedReader(File, Charset)
     */
    public static java.io.BufferedReader newBufferedReader(final Path path, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(path, cs.path);

        // A path on the default file system IS a File, and the File twin already answers a directory, an absent
        // file and a malformed byte the documented way - so hand it over rather than re-implement those rules.
        if (FileSystems.getDefault().equals(path.getFileSystem())) {
            return newBufferedReader(path.toFile(), charset);
        }

        // Another provider: build the same three answers by hand. The directory check is eager here (one stat)
        // because a provider may open a directory successfully and fail only on read, which is too late to
        // classify.
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("'" + path + "' is a directory, not a file");
        }

        final InputStream is;

        try {
            is = Files.newInputStream(path);
        } catch (final NoSuchFileException e) {
            final FileNotFoundException absent = new FileNotFoundException("'" + path + "' does not exist");
            absent.initCause(e);

            throw new UncheckedIOException(absent);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        // InputStreamReader, not Files.newBufferedReader's decoder: it replaces malformed input like every other
        // decoder in this class.
        return new java.io.BufferedReader(newInputStreamReader(is, charset));
    }

    /**
     * Creates a new BufferedReader instance for the specified InputStream with the default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.txt");
     *      BufferedReader br = IOUtil.newBufferedReader(is)) {
     *     String line = br.readLine();
     * }
     * }</pre>
     *
     * @param is the InputStream to be read from.
     * @return a new BufferedReader instance; wrapping does no I/O, so a closed or unreadable stream fails on the
     *         first read, not here.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @see #newInputStreamReader(InputStream)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final InputStream is) throws IllegalArgumentException {
        return new java.io.BufferedReader(newInputStreamReader(is, DEFAULT_CHARSET)); // NOSONAR
    }

    /**
     * Creates a new BufferedReader instance for the specified InputStream with a specified charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("file.txt");
     *      BufferedReader br = IOUtil.newBufferedReader(is, StandardCharsets.UTF_8)) {
     *     String line = br.readLine();
     * }
     * }</pre>
     *
     * @param is      the InputStream to be read from.
     * @param charset the charset to be used; {@code null} uses the default charset (UTF-8).
     * @return a new BufferedReader instance; wrapping does no I/O, so a closed or unreadable stream fails on the
     *         first read, not here.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @see #newInputStreamReader(InputStream, Charset)
     * @see java.io.BufferedReader#BufferedReader(Reader)
     */
    public static java.io.BufferedReader newBufferedReader(final InputStream is, final Charset charset) throws IllegalArgumentException {
        return new java.io.BufferedReader(newInputStreamReader(is, checkCharset(charset)));
    }

    /**
     * Creates a new BufferedWriter instance that wraps the specified Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer writer = new FileWriter("output.txt");
     *      BufferedWriter bw = IOUtil.newBufferedWriter(writer)) {
     *     bw.write("Line 1");
     *     bw.newLine();
     * }
     * }</pre>
     *
     * @param writer the Writer to be wrapped. It must not be {@code null}.
     * @return a new BufferedWriter instance.
     * @throws IllegalArgumentException if {@code writer} is {@code null}.
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final Writer writer) throws IllegalArgumentException {
        N.checkArgNotNull(writer, cs.writer);

        return new java.io.BufferedWriter(writer);
    }

    /**
     * Creates a new BufferedWriter instance for the specified file path and default charset.
     *
     * @param filePath the path of the file to be written to. It is created, along with any missing parent directories, if it does not exist; otherwise it is truncated.
     * @return a new BufferedWriter instance.
     * @throws IllegalArgumentException if {@code filePath} is {@code null}
     * @throws UncheckedIOException if opening {@code filePath} for writing fails
     * @see #newBufferedWriter(File)
     */
    static java.io.BufferedWriter newBufferedWriter(final String filePath) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(filePath, cs.filePath);

        return newBufferedWriter(new File(filePath));
    }

    /**
     * Creates a new BufferedWriter instance for the specified file and default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("output.txt");
     * try (BufferedWriter bw = IOUtil.newBufferedWriter(file)) {
     *     bw.write("Hello");
     *     bw.newLine();
     * }
     * }</pre>
     *
     * @param file the file to be written to. It is created, along with any missing parent directories, if it does not exist; otherwise it is truncated.
     * @return a new BufferedWriter instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see #newFileWriter(File)
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final File file) throws IllegalArgumentException, UncheckedIOException {
        return new java.io.BufferedWriter(newFileWriter(file));
    }

    /**
     * Creates a new BufferedWriter instance for the specified file and charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("output.txt");
     * try (BufferedWriter bw = IOUtil.newBufferedWriter(file, StandardCharsets.UTF_8)) {
     *     bw.write("UTF-8 text");
     * }
     * }</pre>
     *
     * @param file    the file to be written to. It is created, along with any missing parent directories, if it does not exist; otherwise it is truncated.
     * @param charset the charset to be used for writing to the file; {@code null} uses the default charset (UTF-8).
     * @return a new BufferedWriter instance.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or is a directory rather than a file.
     * @throws UncheckedIOException if opening {@code file} for writing fails
     * @see #newFileWriter(File, Charset)
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final File file, final Charset charset) throws IllegalArgumentException, UncheckedIOException {
        return new java.io.BufferedWriter(newFileWriter(file, checkCharset(charset)));
    }

    /**
     * Creates a new BufferedWriter instance for the specified OutputStream with default charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("output.txt");
     *      BufferedWriter bw = IOUtil.newBufferedWriter(os)) {
     *     bw.write("Content");
     * }
     * }</pre>
     *
     * @param os the OutputStream to be written to.
     * @return a new BufferedWriter instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     * @see #newOutputStreamWriter(OutputStream)
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final OutputStream os) throws IllegalArgumentException {
        return new java.io.BufferedWriter(newOutputStreamWriter(os)); // NOSONAR
    }

    /**
     * Creates a new BufferedWriter instance for the specified OutputStream and Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("output.txt");
     *      BufferedWriter bw = IOUtil.newBufferedWriter(os, StandardCharsets.UTF_8)) {
     *     bw.write("UTF-8");
     * }
     * }</pre>
     *
     * @param os      the OutputStream to be written to.
     * @param charset the Charset to be used for writing to the OutputStream; {@code null} uses the default charset (UTF-8).
     * @return a new BufferedWriter instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     * @see #newOutputStreamWriter(OutputStream, Charset)
     * @see java.io.BufferedWriter#BufferedWriter(Writer)
     */
    public static java.io.BufferedWriter newBufferedWriter(final OutputStream os, final Charset charset) throws IllegalArgumentException {
        return new java.io.BufferedWriter(newOutputStreamWriter(os, checkCharset(charset)));
    }

    /**
     * Creates a new LZ4BlockInputStream instance for the specified InputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.lz4");
     *      LZ4BlockInputStream lz4Is = IOUtil.newLZ4BlockInputStream(is)) {
     *     byte[] data = IOUtil.readAllBytes(lz4Is);
     * }
     * }</pre>
     *
     * @param is the InputStream to be read from. It must not be {@code null}.
     * @return a new LZ4BlockInputStream instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     */
    public static LZ4BlockInputStream newLZ4BlockInputStream(final InputStream is) throws IllegalArgumentException {
        N.checkArgNotNull(is, cs.is);

        return new LZ4BlockInputStream(is);
    }

    /**
     * Creates a new LZ4BlockOutputStream instance for the specified OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("data.lz4");
     *      LZ4BlockOutputStream lz4Os = IOUtil.newLZ4BlockOutputStream(os)) {
     *     lz4Os.write("Compressed data".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os the OutputStream to be written to. It must not be {@code null}.
     * @return a new LZ4BlockOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new LZ4BlockOutputStream(os);
    }

    /**
     * Creates a new LZ4BlockOutputStream instance for the specified OutputStream with the given block size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("data.lz4");
     *      LZ4BlockOutputStream lz4Os = IOUtil.newLZ4BlockOutputStream(os, 8192)) {
     *     lz4Os.write("Compressed with custom block size".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os        the OutputStream to be written to. It must not be {@code null}.
     * @param blockSize the block size for the LZ4BlockOutputStream. The LZ4 codec enforces a lower bound of 64
     *                  bytes and an upper bound of its own; the exact bounds belong to that library, not to
     *                  this class.
     * @return a new LZ4BlockOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}, or if the LZ4 codec rejects
     *         {@code blockSize}.
     */
    public static LZ4BlockOutputStream newLZ4BlockOutputStream(final OutputStream os, final int blockSize) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new LZ4BlockOutputStream(os, blockSize);
    }

    /**
     * Creates a new SnappyInputStream instance for the specified InputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.snappy");
     *      SnappyInputStream snappyIs = IOUtil.newSnappyInputStream(is)) {
     *     byte[] data = IOUtil.readAllBytes(snappyIs);
     * }
     * }</pre>
     *
     * @param is the InputStream to be read from. It must not be {@code null}.
     * @return a new SnappyInputStream instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @throws UncheckedIOException if initializing the Snappy decoder for {@code is} fails
     */
    public static SnappyInputStream newSnappyInputStream(final InputStream is) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(is, cs.is);

        try {
            return new SnappyInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new SnappyOutputStream instance for the specified OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("data.snappy");
     *      SnappyOutputStream snappyOs = IOUtil.newSnappyOutputStream(os)) {
     *     snappyOs.write("Snappy compressed".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os the OutputStream to be written to. It must not be {@code null}.
     * @return a new SnappyOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new SnappyOutputStream(os);
    }

    /**
     * Creates a new SnappyOutputStream instance with the specified OutputStream and buffer size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("data.snappy");
     *      SnappyOutputStream snappyOs = IOUtil.newSnappyOutputStream(os, 8192)) {
     *     snappyOs.write("Snappy with buffer".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os         the OutputStream to be written to. It must not be {@code null}.
     * @param bufferSize the size of the buffer to be used. The Snappy codec accepts a limited range only
     *                   (1&nbsp;KiB to 512&nbsp;MiB at the time of writing) and rejects anything outside it;
     *                   the exact bounds belong to that library, not to this class.
     * @return a new SnappyOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}, or if the Snappy codec rejects
     *         {@code bufferSize}.
     */
    public static SnappyOutputStream newSnappyOutputStream(final OutputStream os, final int bufferSize) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new SnappyOutputStream(os, bufferSize);
    }

    /**
     * Creates a new GZIPInputStream instance for the specified InputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.gz");
     *      GZIPInputStream gzipIs = IOUtil.newGZIPInputStream(is)) {
     *     byte[] data = IOUtil.readAllBytes(gzipIs);
     * }
     * }</pre>
     *
     * @param is the InputStream to be read from. It must not be {@code null}.
     * @return a new GZIPInputStream instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @throws UncheckedIOException if reading the GZIP header from {@code is} fails or the header is not valid GZIP data
     */
    public static GZIPInputStream newGZIPInputStream(final InputStream is) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(is, cs.is);

        try {
            return new GZIPInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new GZIPInputStream instance with the specified InputStream and buffer size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.gz");
     *      GZIPInputStream gzipIs = IOUtil.newGZIPInputStream(is, 8192)) {
     *     byte[] data = IOUtil.readAllBytes(gzipIs);
     * }
     * }</pre>
     *
     * @param is         the InputStream to be read from. It must not be {@code null}.
     * @param bufferSize the size of the buffer to be used; must be positive.
     * @return a new GZIPInputStream instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}, or if {@code bufferSize} is not positive.
     * @throws UncheckedIOException if reading the GZIP header from {@code is} fails or the header is not valid GZIP data
     */
    public static GZIPInputStream newGZIPInputStream(final InputStream is, final int bufferSize) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(is, cs.is);
        N.checkArgPositive(bufferSize, cs.bufferSize);

        try {
            return new GZIPInputStream(is, bufferSize);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new GZIPOutputStream instance for the specified OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("data.gz");
     *      GZIPOutputStream gzipOs = IOUtil.newGZIPOutputStream(os)) {
     *     gzipOs.write("GZIP compressed data".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os the OutputStream to be written to. It must not be {@code null}.
     * @return a new GZIPOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     * @throws UncheckedIOException if writing the initial GZIP header to {@code os} fails
     */
    public static GZIPOutputStream newGZIPOutputStream(final OutputStream os) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(os, cs.os);

        try {
            return new GZIPOutputStream(os);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new GZIPOutputStream with the specified OutputStream and buffer size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("data.gz");
     *      GZIPOutputStream gzipOs = IOUtil.newGZIPOutputStream(os, 8192)) {
     *     gzipOs.write("GZIP with buffer".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os         the OutputStream to be written to. It must not be {@code null}.
     * @param bufferSize the size of the buffer to be used; must be positive.
     * @return a new GZIPOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}, or if {@code bufferSize} is not positive.
     * @throws UncheckedIOException if writing the initial GZIP header to {@code os} fails
     */
    public static GZIPOutputStream newGZIPOutputStream(final OutputStream os, final int bufferSize) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(os, cs.os);
        N.checkArgPositive(bufferSize, cs.bufferSize);

        try {
            return new GZIPOutputStream(os, bufferSize);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Creates a new ZipInputStream with the specified InputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("archive.zip");
     *      ZipInputStream zipIs = IOUtil.newZipInputStream(is)) {
     *     ZipEntry entry = zipIs.getNextEntry();
     *     byte[] data = IOUtil.readAllBytes(zipIs);
     * }
     * }</pre>
     *
     * @param is the InputStream to be used for creating the ZipInputStream. It must not be {@code null}.
     * @return a new ZipInputStream instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @see ZipInputStream#ZipInputStream(InputStream)
     */
    public static ZipInputStream newZipInputStream(final InputStream is) throws IllegalArgumentException {
        N.checkArgNotNull(is, cs.is);

        return new ZipInputStream(is);
    }

    /**
     * Creates a new ZipInputStream with the specified InputStream and Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("archive.zip");
     *      ZipInputStream zipIs = IOUtil.newZipInputStream(is, StandardCharsets.UTF_8)) {
     *     ZipEntry entry = zipIs.getNextEntry();
     * }
     * }</pre>
     *
     * @param is      the InputStream to be used for creating the ZipInputStream. It must not be {@code null}.
     * @param charset the Charset to be used for decoding entry names and comments; {@code null} uses the default charset (UTF-8).
     * @return a new ZipInputStream instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @see ZipInputStream#ZipInputStream(InputStream, Charset)
     */
    public static ZipInputStream newZipInputStream(final InputStream is, final Charset charset) throws IllegalArgumentException {
        N.checkArgNotNull(is, cs.is);

        return new ZipInputStream(is, checkCharset(charset));
    }

    /**
     * Creates a new ZipOutputStream with the specified OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("archive.zip");
     *      ZipOutputStream zipOs = IOUtil.newZipOutputStream(os)) {
     *     zipOs.putNextEntry(new ZipEntry("file.txt"));
     *     zipOs.write("Content".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os the OutputStream to be used for creating the ZipOutputStream. It must not be {@code null}.
     * @return a new ZipOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     * @see ZipOutputStream#ZipOutputStream(OutputStream)
     */
    public static ZipOutputStream newZipOutputStream(final OutputStream os) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new ZipOutputStream(os);
    }

    /**
     * Creates a new ZipOutputStream with the specified OutputStream and Charset.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = new FileOutputStream("archive.zip");
     *      ZipOutputStream zipOs = IOUtil.newZipOutputStream(os, StandardCharsets.UTF_8)) {
     *     zipOs.putNextEntry(new ZipEntry("file.txt"));
     *     zipOs.write("UTF-8 content".getBytes(StandardCharsets.UTF_8));
     * }
     * }</pre>
     *
     * @param os      the OutputStream to be used for creating the ZipOutputStream. It must not be {@code null}.
     * @param charset the Charset to be used for encoding entry names and comments; {@code null} uses the default charset (UTF-8).
     * @return a new ZipOutputStream instance.
     * @throws IllegalArgumentException if {@code os} is {@code null}.
     * @see ZipOutputStream#ZipOutputStream(OutputStream, Charset)
     */
    public static ZipOutputStream newZipOutputStream(final OutputStream os, final Charset charset) throws IllegalArgumentException {
        N.checkArgNotNull(os, cs.os);

        return new ZipOutputStream(os, checkCharset(charset));
    }

    /**
     * Creates a new BrotliInputStream instance for the specified input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.br");
     *      BrotliInputStream brotliIs = IOUtil.newBrotliInputStream(is)) {
     *     byte[] data = IOUtil.readAllBytes(brotliIs);
     * }
     * }</pre>
     *
     * @param is the input stream to be used for creating the BrotliInputStream. It must not be {@code null}.
     * @return a new BrotliInputStream instance.
     * @throws IllegalArgumentException if {@code is} is {@code null}.
     * @throws UncheckedIOException if initializing the Brotli decoder from {@code is} fails
     */
    public static BrotliInputStream newBrotliInputStream(final InputStream is) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(is, cs.is);

        try {
            return new BrotliInputStream(is);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Closes the provided {@code URLConnection} by calling {@link HttpURLConnection#disconnect()} if it is an {@code HttpURLConnection}; otherwise does nothing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URLConnection conn = new URL("http://example.com").openConnection();   // may throw IOException
     * try {
     *     try (InputStream input = conn.getInputStream()) {
     *         byte[] responseBody = IOUtil.readAllBytes(input);
     *     }
     * } finally {
     *     IOUtil.close(conn);   // disconnects if it is an HttpURLConnection; otherwise no-op
     * }
     * }</pre>
     *
     * @param conn the connection to close.
     */
    public static void close(final URLConnection conn) {
        if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection) conn).disconnect();
        }
    }

    /**
     * Closes the provided {@code AutoCloseable} object.
     * <p>
     * If an exception occurs during the close operation, it is wrapped in a {@code RuntimeException}
     * and rethrown. If the object is {@code null}, this method does nothing.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream is = new FileInputStream("file.txt");
     * try {
     *     // Use the stream
     * } finally {
     *     IOUtil.close(is);  // Safely closes and throws RuntimeException if an error occurs
     * }
     * }</pre>
     *
     * @param closeable the AutoCloseable object to be closed. It can be {@code null}.
     * @throws RuntimeException if {@code closeable.close()} throws an exception; checked exceptions are wrapped and runtime exceptions are
     *         propagated
     * @see #closeQuietly(AutoCloseable)
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
     * Closes the provided {@code AutoCloseable} object and handles any exceptions that occur during the closing operation
     * using the specified {@code exceptionHandler}.
     * <p>
     * If the object is {@code null}, this method does nothing.
     * An {@link InterruptedException} restores the current thread's interrupt status before the handler runs,
     * as it does in {@link #closeQuietly(AutoCloseable)} and {@link #closeAll(Iterable)}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream is = new FileInputStream("file.txt");
     * try {
     *     // Use the stream
     * } finally {
     *     IOUtil.close(is, e -> logger.error("Failed to close stream", e));
     * }
     * }</pre>
     *
     * @param closeable        the AutoCloseable object to be closed. It can be {@code null}.
     * @param exceptionHandler the Consumer to handle any exceptions thrown during the close operation.
     * @throws IllegalArgumentException if {@code exceptionHandler} is {@code null}.
     */
    public static void close(final AutoCloseable closeable, final Consumer<Exception> exceptionHandler) throws IllegalArgumentException {
        N.checkArgNotNull(exceptionHandler, cs.exceptionHandler);

        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception e) {
                if (e instanceof InterruptedException) {
                    // As closeQuietly and closeAll do: the handler is free to log and move on, and losing the
                    // interrupt on the way would leave the thread unable to notice the cancellation.
                    Thread.currentThread().interrupt();
                }

                exceptionHandler.accept(e);
            }
        }
    }

    /**
     * Closes all provided {@code AutoCloseable} objects.
     * <p>
     * If an exception occurs while closing any of the objects, the first exception encountered
     * is wrapped in a {@code RuntimeException} and rethrown, with any subsequent exceptions
     * added as suppressed exceptions. If an object is {@code null}, it is ignored.
     * An {@link InterruptedException} from any close operation restores the current thread's interrupt status.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream a = new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8));
     * InputStream b = new ByteArrayInputStream("y".getBytes(StandardCharsets.UTF_8));
     * IOUtil.closeAll(a, b, null);   // closes a and b in order; null is ignored
     * IOUtil.closeAll();             // empty varargs: no-op
     * }</pre>
     *
     * @param closeables the AutoCloseable objects to be closed. It may contain {@code null} elements.
     * @throws RuntimeException if closing a non-null element of {@code closeables} throws an exception; the first exception is propagated or
     *         wrapped after all elements are attempted
     */
    @SafeVarargs
    public static void closeAll(final AutoCloseable... closeables) {
        if (N.isEmpty(closeables)) {
            return;
        }

        closeAll(Arrays.asList(closeables));
    }

    /**
     * Closes all provided {@code AutoCloseable} objects in the {@code Iterable}.
     * <p>
     * If an exception occurs while closing any of the objects, the first exception encountered
     * is wrapped in a {@code RuntimeException} and rethrown, with any subsequent exceptions
     * added as suppressed exceptions. If an object is {@code null}, it is ignored.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<AutoCloseable> closeables = Arrays.asList(
     *         new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)),
     *         new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8)));
     * IOUtil.closeAll(closeables);                               // closes all in order
     * IOUtil.closeAll(Collections.<AutoCloseable>emptyList());   // empty collection, no-op
     * }</pre>
     *
     * @param closeables the Iterable of AutoCloseable objects to be closed. It may contain {@code null} elements.
     * @throws RuntimeException if closing a non-null element of {@code closeables} throws an exception; the first exception is propagated or
     *         wrapped after all elements are attempted
     */
    public static void closeAll(final Iterable<? extends AutoCloseable> closeables) {
        if (N.isEmptyCollection(closeables)) {
            return;
        }

        Exception ex = null;

        for (final AutoCloseable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (final Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (ex == null) {
                    ex = e;
                } else if (ex != e) {
                    // A custom closeable may throw the same cached exception instance on
                    // repeated close attempts. Throwable rejects self-suppression, but that
                    // must not prevent the remaining closeables from being processed.
                    ex.addSuppressed(e);
                }
            }
        }

        if (ex != null) {
            throw ExceptionUtil.toRuntimeException(ex, true);
        }
    }

    /**
     * Closes the provided {@code AutoCloseable} object quietly, suppressing any exceptions.
     * <p>
     * If an exception occurs during the close operation, it is logged at error level but not rethrown.
     * An {@link InterruptedException} restores the current thread's interrupt status.
     * If the object is {@code null}, this method does nothing.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream is = null;
     * try {
     *     is = new FileInputStream("file.txt");
     *     // Use the stream
     * } finally {
     *     IOUtil.closeQuietly(is);  // Closes silently, ignoring exceptions but logging them
     * }
     * }</pre>
     *
     * @param closeable the AutoCloseable object to be closed. It can be {@code null}.
     * @see #close(AutoCloseable)
     */
    public static void closeQuietly(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                logger.error(e, "Failed to close {}", ClassUtil.getSimpleClassName(closeable.getClass()));
            }
        }
    }

    /**
     * Closes {@code closeable} while {@code primary} is already propagating, attaching any close failure to it
     * as a suppressed exception instead of discarding it.
     *
     * <p>This is the cleanup path for a resource that was opened successfully but whose enclosing construction
     * then failed, where try-with-resources cannot be used because the resource is handed off on success.
     *
     * @param closeable the resource to close; may be {@code null}.
     * @param primary   the failure that is already in flight.
     */
    private static void closeSuppressing(final AutoCloseable closeable, final Throwable primary) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (final Exception e) {
            if (primary != e) {
                primary.addSuppressed(e);
            }
        }
    }

    /**
     * Closes all provided AutoCloseable objects quietly.
     * Any exceptions that occur during the closing operation are logged but not rethrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream a = new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8));
     * AutoCloseable failing = () -> { throw new IOException("boom"); };
     * IOUtil.closeAllQuietly(a, failing, null);   // closes a; the thrown exception is suppressed; null ignored
     * IOUtil.closeAllQuietly();                   // empty varargs: no-op
     * }</pre>
     *
     * @param closeables the AutoCloseable objects to be closed. It may contain {@code null} elements.
     */
    @SafeVarargs
    public static void closeAllQuietly(final AutoCloseable... closeables) {
        if (N.isEmpty(closeables)) {
            return;
        }

        closeAllQuietly(Arrays.asList(closeables));
    }

    /**
     * Closes all provided AutoCloseable objects quietly.
     * Any exceptions that occur during the closing operation are logged but not rethrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<AutoCloseable> closeables = Arrays.asList(
     *         new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)),
     *         new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8)));
     * IOUtil.closeAllQuietly(closeables);                               // closes all, suppresses exceptions
     * IOUtil.closeAllQuietly(Collections.<AutoCloseable>emptyList());   // empty collection, no-op
     * }</pre>
     *
     * @param closeables the Iterable of AutoCloseable objects to be closed. It may contain {@code null} elements.
     */
    public static void closeAllQuietly(final Iterable<? extends AutoCloseable> closeables) {
        if (N.isEmptyCollection(closeables)) {
            return;
        }

        for (final AutoCloseable closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     *
     * <p>The source path is resolved (a top-level symlink, to a file or a directory, is followed so that the
     * referenced content is copied) but the copy keeps the <i>link's own name</i>, as {@code zip} and
     * {@code moveToDirectory} do: {@code current.log -> app-2026.log} is copied as {@code current.log}.
     * Nested symbolic links are copied as links and are not followed; a nested Windows directory junction, which
     * cannot be recreated as a link, is followed and copied as a plain directory - unless it leads back to a
     * directory on the path being copied (directly or through other junctions) or reaches the copy's destination,
     * a directory above it or anything inside it, in which case it is left out, as a dangling junction and a
     * nested special file (FIFO, socket, device node) are left out; any other junction is followed, so its target
     * may be copied more than once. Permissions are not carried over: every
     * copied file gets default permissions, where {@link #copyFile(File, File)} preserves them.</p>
     *
     * <p>Existing files in the destination are never overwritten: an {@code IOException} is thrown
     * if a destination file already exists, or if a file or a link of any kind (a live directory link included:
     * a copy never writes through one) sits where a copied subdirectory would be created. If a file is copied
     * into its own parent directory,
     * the copy is created under the name {@code "Copy of " + fileName} - inspect the returned {@code File}
     * rather than assuming {@code destDir/srcFile.getName()}. A <i>directory</i> has no such fallback: a copy
     * whose target inside {@code destDir} would be the source directory itself, or one of its ancestors, is
     * rejected with {@code IllegalArgumentException} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File sourceFile = new File("document.pdf");
     * File targetDir = new File("backups");
     * File copy = IOUtil.copyToDirectory(sourceFile, targetDir);
     * // copy is backups/document.pdf
     * }</pre>
     *
     * @param srcFile the source file or directory to be copied. It must not be {@code null}.
     * @param destDir the destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @return the file or directory actually created inside {@code destDir}, whose own path is resolved to its
     *         canonical form (so a {@code destDir} containing {@code .} or a symlink comes back resolved). This is
     *         normally {@code new File(destDir, srcFile.getName())} - a symlink source keeps its own name - but it
     *         carries the {@code "Copy of "} prefix when a file, or a link to one, is copied into the directory that
     *         already holds it. It is never the source itself - a copy that would land there is rejected rather
     *         than reported as done.
     * @throws IllegalArgumentException if {@code srcFile} is {@code null}, if {@code destDir} is {@code null} or
     *         exists but is not a directory, if the destination directory is inside or the same as the source
     *         directory, or if the directory the source would be recreated as is the source itself or one of its
     *         ancestors.
     * @throws FileNotFoundException if {@code srcFile} does not exist, cannot be read, or is neither a file nor a
     *         directory.
     * @throws IOException if opening or reading {@code srcFile}, creating entries in {@code destDir}, or writing their contents fails
     */
    public static File copyToDirectory(final File srcFile, final File destDir) throws IllegalArgumentException, IOException {
        return copyToDirectory(srcFile, destDir, true);
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     * If the source is a directory, it is recreated (by its own name) inside the destination directory,
     * along with all of its contents.
     *
     * <p>The source path is resolved (a top-level symlink, to a file or a directory, is followed so that the
     * referenced content is copied) but the copy keeps the <i>link's own name</i>, as {@code zip} and
     * {@code moveToDirectory} do: {@code current.log -> app-2026.log} is copied as {@code current.log}.
     * Nested symbolic links are copied as links and are not followed; a nested Windows directory junction, which
     * cannot be recreated as a link, is followed and copied as a plain directory - unless it leads back to a
     * directory on the path being copied (directly or through other junctions) or reaches the copy's destination,
     * a directory above it or anything inside it, in which case it is left out, as a dangling junction and a
     * nested special file (FIFO, socket, device node) are left out; any other junction is followed, so its target
     * may be copied more than once. Permissions are not carried over: every
     * copied file gets default permissions, where {@link #copyFile(File, File)} preserves them.</p>
     *
     * <p>Existing files in the destination are never overwritten: an {@code IOException} is thrown
     * if a destination file already exists, or if a file or a link of any kind (a live directory link included:
     * a copy never writes through one) sits where a copied subdirectory would be created. If a file is copied
     * into its own parent directory,
     * the copy is created under the name {@code "Copy of " + fileName} - inspect the returned {@code File}
     * rather than assuming {@code destDir/srcFile.getName()}. A <i>directory</i> has no such fallback: a copy
     * whose target inside {@code destDir} would be the source directory itself, or one of its ancestors, is
     * rejected with {@code IllegalArgumentException} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File sourceFile = new File("document.pdf");
     * File targetDir = new File("backups");
     * File copy = IOUtil.copyToDirectory(sourceFile, targetDir, true);
     * // copy is backups/document.pdf, with the modification date preserved
     * }</pre>
     *
     * @param srcFile          the source file or directory to be copied. It must not be {@code null}.
     * @param destDir          the destination directory where the source file or directory will be copied to. It must not be {@code null}.
     * @param preserveFileDate if {@code true}, the last modified date of the file will be preserved in the copied file.
     * @return the file or directory actually created inside {@code destDir}, whose own path is resolved to its
     *         canonical form (so a {@code destDir} containing {@code .} or a symlink comes back resolved). This is
     *         normally {@code new File(destDir, srcFile.getName())} - a symlink source keeps its own name - but it
     *         carries the {@code "Copy of "} prefix when a file, or a link to one, is copied into the directory that
     *         already holds it. It is never the source itself - a copy that would land there is rejected rather
     *         than reported as done.
     * @throws IllegalArgumentException if {@code srcFile} is {@code null}, if {@code destDir} is {@code null} or
     *         exists but is not a directory, if the destination directory is inside or the same as the source
     *         directory, or if the directory the source would be recreated as is the source itself or one of its
     *         ancestors.
     * @throws FileNotFoundException if {@code srcFile} does not exist, cannot be read, or is neither a file nor a
     *         directory.
     * @throws IOException if opening or reading {@code srcFile}, creating entries in {@code destDir}, or writing their contents fails
     */
    public static File copyToDirectory(final File srcFile, final File destDir, final boolean preserveFileDate) throws IllegalArgumentException, IOException {
        return copyToDirectory(srcFile, destDir, preserveFileDate, BiPredicates.alwaysTrue());
    }

    /**
     * Copies the specified source file or directory to the specified destination directory.
     * If the source is a directory, it is recreated (by its own name) inside the destination directory,
     * along with all of its contents.
     *
     * <p>The source path is resolved (a top-level symlink, to a file or a directory, is followed so that the
     * referenced content is copied) but the copy keeps the <i>link's own name</i>, as {@code zip} and
     * {@code moveToDirectory} do: {@code current.log -> app-2026.log} is copied as {@code current.log}.
     * Nested symbolic links are copied as links and are not followed; a nested Windows directory junction, which
     * cannot be recreated as a link, is followed and copied as a plain directory - unless it leads back to a
     * directory on the path being copied (directly or through other junctions) or reaches the copy's destination,
     * a directory above it or anything inside it, in which case it is left out, as a dangling junction and a
     * nested special file (FIFO, socket, device node) are left out; any other junction is followed, so its target
     * may be copied more than once. Permissions are not carried over: every
     * copied file gets default permissions, where {@link #copyFile(File, File)} preserves them.</p>
     *
     * <p>Existing files in the destination are never overwritten: an {@code IOException} is thrown
     * if a destination file already exists, or if a file or a link of any kind (a live directory link included:
     * a copy never writes through one) sits where a copied subdirectory would be created. If a file is copied
     * into its own parent directory,
     * the copy is created under the name {@code "Copy of " + fileName} - inspect the returned {@code File}
     * rather than assuming {@code destDir/srcFile.getName()}. A <i>directory</i> has no such fallback: a copy
     * whose target inside {@code destDir} would be the source directory itself, or one of its ancestors, is
     * rejected with {@code IllegalArgumentException} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume "input" is a directory containing "keep.txt" and "skip.log",
     * // and "backup" is an existing destination directory.
     * File srcDir = new File("input");
     * File destDir = new File("backup");
     * // copy only files whose name ends with ".txt"
     * File copiedDir = IOUtil.copyToDirectory(srcDir, destDir, false, (parent, file) -> file.getName().endsWith(".txt"));
     * // copiedDir is destDir/<srcDir-name>, which now holds keep.txt; skip.log is not copied
     * }</pre>
     *
     * @param <E>              the type of the exception that may be thrown by the filter.
     * @param srcFile          the source file or directory to be copied. It must not be {@code null}.
     * @param destDir          the destination directory where the source file or directory will be copied to. It must not be {@code null}.
     *                         It is created only after every argument has been validated, so a rejected call never leaves a new directory behind.
     * @param preserveFileDate if {@code true}, the last modified date of the file will be preserved in the copied file.
     * @param filter           a BiPredicate that takes the source directory and the file being evaluated as
     *                         arguments and returns a boolean. If the predicate returns {@code true}, the file is
     *                         copied; if it returns {@code false}, the file is not copied. It selects the source's
     *                         <i>contents</i> and is never asked about {@code srcFile} itself, so a plain-file source
     *                         is copied whatever the filter answers (the same convention as
     *                         {@link #deleteFilesFromDirectory(File, Throwables.BiPredicate)}). A rejected
     *                         subdirectory is still descended into, so that its own matching entries are copied -
     *                         which means the destination mirrors the source's directory structure in full,
     *                         including directories that ended up holding nothing.
     * @return the file or directory actually created inside {@code destDir}. This is normally
     *         {@code new File(destDir, srcFile.getName())} (with {@code destDir} resolved to its canonical form; a
     *         symlink source keeps its own name), but it carries the {@code "Copy of "} prefix when a file, or a link
     *         to one, is copied into the directory that already holds it. It is never the source itself - a copy that
     *         would land there is rejected rather than reported as done.
     * @throws IllegalArgumentException if {@code srcFile} or {@code filter} is {@code null}, if {@code destDir} is
     *         {@code null} or exists but is not a directory, if the destination directory is inside or the
     *         same as the source directory, or if the directory the source would be recreated as is the source
     *         itself or one of its ancestors.
     * @throws FileNotFoundException if {@code srcFile} does not exist, cannot be read, or is neither a file nor a
     *         directory.
     * @throws IOException if opening or reading {@code srcFile}, creating entries in {@code destDir}, or writing their contents fails
     * @throws E if the filter throws an exception.
     */
    public static <E extends Exception> File copyToDirectory(final File srcFile, final File destDir, final boolean preserveFileDate,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws IllegalArgumentException, IOException, E {
        return copyToDirectory(srcFile, destDir, preserveFileDate, filter, null);
    }

    /**
     * The core of every {@code copyToDirectory} overload. {@code guard} is the {@link CopyCycleGuard} of an enclosing
     * {@code copyDirectory} walk, whose immediate children come through here, or {@code null} to start a walk.
     */
    private static <E extends Exception> File copyToDirectory(File srcFile, File destDir, final boolean preserveFileDate,
            final Throwables.BiPredicate<? super File, ? super File, E> filter, final CopyCycleGuard guard) throws IllegalArgumentException, IOException, E {
        checkFileExists(srcFile, true, "srcFile");
        // Validate only; the destination is created below, after every remaining check has passed, so that a
        // rejected call cannot leave a freshly created directory behind (possibly inside the source tree).
        checkDestDirectory(destDir);
        N.checkArgNotNull(filter, cs.filter);

        // The copy is created under the CALLER's name, taken before the source is canonicalized below.
        // Canonicalizing a symbolic link yields its target, and naming the copy after the target silently renamed
        // it: "current.log -> app-2026.log" arrived in destDir as "app-2026.log", and a directory link "config ->
        // config.v2" as "config.v2" - while zip(..) and moveToDirectory(..) keep the link's own name for the same
        // source. The content still comes from the resolved target; only the name is the caller's.
        final String srcName = sourceName(srcFile);

        srcFile = srcFile.getCanonicalFile();
        destDir = destDir.getCanonicalFile();

        final String srcCanonicalPath = srcFile.getCanonicalPath();

        if (srcFile.isDirectory()) {
            // Containment is decided by where the directories actually sit. getCanonicalPath does not
            // follow a Windows junction, so a destDir that is a junction inside the source pointing
            // outside used to be rejected, and one outside pointing back in used to be accepted.
            final Path srcLocation = srcFile.toPath().toRealPath();
            final Path destLocation = resolvedDirectoryLocation(destDir);

            requireDestDirectoryOutsideSourceDirectory(srcLocation.toString(), destLocation.toString(), "copy");

            final File targetDir = new File(destLocation.toFile(), srcName);

            // Checked before createDestDirectory, so a rejected call still leaves no new directory behind.
            requireCopyTargetOutsideSourceDirectory(srcLocation.toString(), destLocation.resolve(srcName).toString());

            createDestDirectory(destLocation.toFile());

            // The walk carries the source's REAL path: a junction met below is left out when it leads back to a
            // directory on that path or reaches the copy's own output (see CopyCycleGuard).
            doCopyDirectory(srcFile, targetDir, srcLocation, preserveFileDate, filter, guard != null ? guard : new CopyCycleGuard(srcLocation));

            return targetDir;
        } else {
            createDestDirectory(destDir);

            final File sameName = new File(destDir, srcName);
            final File destFile;

            // "Copy of" whenever destDir/<name> IS the source: the ordinary file copied into its own directory,
            // a link copied into the directory that holds it (destDir/<name> is the link and resolves to the same
            // target), and a destination that already holds a link back to the source under that name - copying
            // through such a link would have overwritten the source with itself. Decided canonically, because that
            // is what makes two paths the same file.
            if (sameName.getCanonicalPath().equals(srcCanonicalPath)) {
                destFile = new File(destDir, "Copy of " + srcName);
            } else {
                destFile = sameName;
            }

            doCopyFile(srcFile, destFile, preserveFileDate);

            return destFile;
        }
    }

    /**
     * The name a source keeps when it is copied into a directory: the last element of its real path, resolved
     * <i>without following a final symbolic link</i>.
     *
     * <p>{@code toRealPath(NOFOLLOW_LINKS)} does everything canonicalization does - folds {@code "."} and
     * {@code ".."}, leaves every link in the path unresolved, and on Windows restores the on-disk case and expands an
     * 8.3 short name, so {@code new File("x/.")} is still copied as {@code "x"} and {@code "FOO.TXT"} as the
     * {@code foo.txt} that is actually on disk - except that a link as the final element keeps its own name.
     * (A lexically normalized path was tried first; it kept the caller's spelling instead of the disk's, which
     * differed from the old behaviour for case and short names on Windows.) A filesystem root has no name and
     * falls back to the canonical form's (empty) name, exactly as before.
     *
     * @param file the source, already validated to exist.
     * @return the name to copy it under.
     * @throws IOException if the path cannot be resolved.
     */
    private static String sourceName(final File file) throws IOException {
        final Path name;

        try {
            name = file.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).getFileName();
        } catch (final InvalidPathException e) {
            // A name java.io accepts but java.nio does not (a Windows alternate data stream, "f.txt:ads"): the
            // canonical name is what this method always answered for it, and it cannot be a link.
            return file.getCanonicalFile().getName();
        }

        return name == null ? file.getCanonicalFile().getName() : name.toString();
    }

    /**
     * Internal copy directory method.
     *
     * @param <E>              the type of exception that the filter may throw during file filtering.
     * @param srcDir           the validated source directory, must not be {@code null}.
     * @param destDir          the validated destination directory, must not be {@code null}.
     * @param srcDirReal       the real path of {@code srcDir}: its own for a junction that is being followed, the
     *                         parent's real path plus its name for a plain subdirectory (no extra system call).
     * @param preserveFileDate whether to preserve the file date.
     * @param filter           the filter to apply
     * @param guard            the cycle guard of this walk.
     * @throws IOException if listing or reading {@code srcDir}, creating {@code destDir} or its descendants, or copying their contents fails
     * @throws E           if filter throws an exception during file filtering.
     */
    private static <E extends Exception> void doCopyDirectory(final File srcDir, final File destDir, final Path srcDirReal, final boolean preserveFileDate,
            final Throwables.BiPredicate<? super File, ? super File, E> filter, final CopyCycleGuard guard) throws IOException, E {

        // destDir here is always a NESTED destination - the caller's own directory argument was validated and
        // created before the first call - so an entry already there is an overwrite, not a bad argument.
        if (destinationEntryExists(destDir)) {
            // A link, live or dangling, is an entry of its own: copying "through" a live directory link wrote the
            // tree wherever the link pointed - outside destDir, or back INTO the source tree, past the containment
            // check that sees only the caller's destDir - and reported it as created inside destDir. doCopyFile
            // already refuses a file link the same way; GNU cp refuses both ("cannot overwrite non-directory").
            if (isSymbolicLinkOrJunction(destDir)) {
                throw new IOException("The destination already exists as a link, which a copy never writes through: " + describe(destDir));
            }

            // A file: refused as doCopyFile refuses one. createDestDirectory reported it as an
            // IllegalArgumentException naming a path the caller never passed, after the entries before it had
            // already been copied.
            if (!destDir.isDirectory()) {
                throw new IOException("The destination file already exists: " + describe(destDir));
            }
        }

        createDestDirectory(destDir);

        if (guard.outputRoot == null) {
            // The first directory created is the output root (destDir/<name> for copyToDirectory); its real path is
            // what a junction met below must not reach. Canonicalised now that it exists: a path that did not exist
            // canonicalises to the caller's spelling, alias and all. copyDirectory seeds its own root before the walk.
            guard.outputRoot = destDir.getCanonicalFile().toPath().toRealPath();
        }

        final File[] subFiles = srcDir.listFiles();

        // listFiles() returns null on an I/O error (e.g. unreadable directory) - that must not be
        // mistaken for an empty directory, and empty directories still need their date preserved below.
        if (subFiles == null) {
            throw new IOException("Failed to list contents of " + describe(srcDir));
        }

        guard.sourcePath.push(srcDirReal);

        try {
            for (final File subFile : subFiles) {
                if (subFile == null) {
                    continue;
                }

                // Nested symbolic links are copied as links and never followed, so a cyclic link
                // cannot loop and a link cannot escape the source tree.
                final boolean isSymlink = Files.isSymbolicLink(subFile.toPath());

                // A Windows junction cannot be recreated as a link: a live one is followed and copied as a plain
                // directory (documented) unless it leads back into the copy - to a directory on the path being
                // copied, or to the output being written - which would copy for ever, and a DANGLING one, which
                // File.isDirectory() and exists() both deny, has nothing to copy. Both are left out, as is a special
                // file (FIFO, socket, device node): it cannot be recreated, and reading a FIFO blocks until a writer
                // appears, which used to hang the copy for ever. zip leaves the same entries out.
                if (!isSymlink && isLeftOutOfCopy(subFile, guard)) {
                    continue;
                }

                if (filter.test(srcDir, subFile)) {
                    final File dest = new File(destDir, subFile.getName());

                    if (isSymlink) {
                        copySymbolicLink(subFile, dest);
                    } else if (subFile.isDirectory()) {
                        // The filter applies to every descendant, not just to entries whose parent
                        // directory happened to be rejected by the filter.
                        doCopyDirectory(subFile, dest, walkedReal(subFile, srcDirReal), preserveFileDate, filter, guard);
                    } else {
                        doCopyFile(subFile, dest, preserveFileDate);
                    }
                } else if (subFile.isDirectory() && !isSymlink) {
                    final File dest = new File(destDir, subFile.getName());
                    doCopyDirectory(subFile, dest, walkedReal(subFile, srcDirReal), preserveFileDate, filter, guard);
                }
            }
        } finally {
            guard.sourcePath.pop();
        }

        // Do this last, as the above has probably affected directory metadata
        if (preserveFileDate) {
            setTimes(srcDir, destDir);
        }
    }

    /**
     * The cycle guard of a directory copy. The copy family follows Windows junctions (Java cannot recreate one), so
     * a junction that leads back to a directory on the path being copied - the holder, an ancestor, or a directory
     * reached through another junction - or that reaches the copy's own output (the output root, a directory above
     * it, or anything inside it: the copy would re-copy what it is writing) must be left out, or the walk runs until
     * the platform's reparse or path-length limit: a mutual pair of junctions used to leave a 64-level duplicate
     * behind and report success, one into the output a 4,000-level tree that took a minute to delete. Real paths
     * are DERIVED, never queried per directory: one {@code toRealPath()} for the source root, one for the output
     * root, one per junction met; a plain subdirectory is its parent's real path plus its name.
     *
     * <p>Paths are compared as spelled, which fails across an ALIAS: a {@code subst} or mapped drive or a UNC share
     * names the same directories under another root, {@code toRealPath()} keeps such a spelling (no reparse point
     * on the path), {@code getCanonicalPath()} resolves a subst drive on JDK 25 but nothing on JDK 21, and a
     * junction's real target is always spelled on the real volume. So when a junction target's root differs from a
     * seed's, the guard learns the alias once by identity - {@link Files#isSameFile} between the target's ancestors
     * and the seed's - and from then on translates targets under that prefix into the seed's spelling before the
     * comparison. Same-root walks, the common case, never pay a system call for it.</p>
     */
    private static final class CopyCycleGuard {
        /** Real paths of the directories on the path being copied, the source root deepest in the deque. */
        private final Deque<Path> sourcePath = new ArrayDeque<>();
        /** Real path of the directory the copy writes, once it exists. */
        private Path outputRoot;
        /** Alias prefixes learnt: a spelling under another root -> the same directory in a seed's spelling. */
        private final Map<Path, Path> aliases = new HashMap<>();

        CopyCycleGuard(final Path sourceRootReal) {
            sourcePath.push(sourceRootReal);
        }

        /** Whether following a junction with this real target would re-enter the copy. */
        boolean leadsBack(final Path junctionTarget) {
            if (leadsBackAsSpelled(junctionTarget)) {
                return true;
            }

            final Path translated = inSeedSpelling(junctionTarget);

            return translated != null && leadsBackAsSpelled(translated);
        }

        private boolean leadsBackAsSpelled(final Path junctionTarget) {
            for (final Path onPath : sourcePath) {
                if (onPath.startsWith(junctionTarget)) {
                    return true;
                }
            }

            return outputRoot != null && (junctionTarget.startsWith(outputRoot) || outputRoot.startsWith(junctionTarget));
        }

        /**
         * {@code target} respelled under a seed's root, when its own root is another spelling of the same place;
         * {@code null} when no seed shares a directory with it.
         */
        private Path inSeedSpelling(final Path target) {
            for (final Map.Entry<Path, Path> alias : aliases.entrySet()) {
                if (target.startsWith(alias.getKey())) {
                    return alias.getValue().resolve(alias.getKey().relativize(target));
                }
            }

            for (final Path seed : new Path[] { sourcePath.peekLast(), outputRoot }) {
                if (seed == null || seed.getRoot() == null || seed.getRoot().equals(target.getRoot())) {
                    continue; // the same root: the spelling comparison was authoritative
                }

                for (Path ancestor = target; ancestor != null; ancestor = ancestor.getParent()) {
                    for (Path seedAncestor = seed; seedAncestor != null; seedAncestor = seedAncestor.getParent()) {
                        if (isSameDirectory(ancestor, seedAncestor)) {
                            aliases.put(ancestor, seedAncestor);

                            return seedAncestor.resolve(ancestor.relativize(target));
                        }
                    }
                }
            }

            return null;
        }

        private static boolean isSameDirectory(final Path a, final Path b) {
            try {
                return Files.isSameFile(a, b);
            } catch (final IOException | SecurityException e) {
                return false;
            }
        }
    }

    /**
     * Whether {@code entry}, which {@code File.exists()} denies, is a reparse point of a kind Java can neither follow
     * nor classify (NOFOLLOW attributes say "other", neither a link nor a directory): the symbolic link WSL writes
     * on a Windows volume without the symlink privilege is one. A copy used to fail on it half-way ("The file cannot
     * be accessed by the system"); it has nothing to copy. An entry that has vanished reads as {@code false}.
     */
    private static boolean isUnfollowableReparsePoint(final File entry) {
        try {
            return Files.readAttributes(entry.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther();
        } catch (final IOException | InvalidPathException e) {
            return false;
        }
    }

    /** The real path a subdirectory is walked under: its own for a junction, the parent's plus its name otherwise. */
    private static Path walkedReal(final File subDir, final Path parentReal) throws IOException {
        return isSymbolicLinkOrJunction(subDir) ? subDir.toPath().toRealPath() : parentReal.resolve(subDir.getName());
    }

    /**
     * Whether a non-link entry of a directory being copied is left out of the copy: a DANGLING junction (nothing to
     * follow, nothing to recreate), a live junction that {@code guard} says leads back into the copy (following it
     * would copy for ever - it used to fail only once the platform's traversal limit was reached, 63 junctions or
     * 32K characters down), or a special file - a FIFO, socket or device node - which cannot be recreated and whose
     * reading may block for ever. Nested symbolic links are handled by the caller before this is asked.
     */
    private static boolean isLeftOutOfCopy(final File entry, final CopyCycleGuard guard) throws IOException {
        if (!entry.exists()) {
            // File.exists() denies a dangling junction, and any other reparse point the platform cannot follow - a
            // symbolic link WSL wrote on a Windows volume, which Java sees as neither a link nor a file; both are
            // left out, as zip leaves them out. A plain entry that vanished is reported by the copy itself.
            return isSymbolicLinkOrJunction(entry) || isUnfollowableReparsePoint(entry);
        }

        if (entry.isDirectory()) {
            return isSymbolicLinkOrJunction(entry) && guard.leadsBack(entry.toPath().toRealPath());
        }

        return !entry.isFile();
    }

    private static void copySymbolicLink(final File srcLink, final File dest) throws IOException {
        if (destinationEntryExists(dest)) {
            throw new IOException("The destination file already exists: " + describe(dest));
        }

        Files.copy(srcLink.toPath(), dest.toPath(), LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * Whether an entry already occupies the copy destination. {@code File.exists()} follows a link and so denies a
     * DANGLING link, and the copy then opened the destination through that link and wrote the file wherever the link
     * pointed - outside {@code destDir}, and reported as "created inside" it. The entry itself is what counts.
     */
    private static boolean destinationEntryExists(final File dest) {
        try {
            return Files.exists(dest.toPath(), LinkOption.NOFOLLOW_LINKS);
        } catch (final InvalidPathException e) {
            return dest.exists();
        }
    }

    private static void doCopyFile(final File srcFile, final File destFile, final boolean preserveFileDate) throws IOException {
        if (destinationEntryExists(destFile)) {
            throw new IOException("The destination file already exists: " + describe(destFile));
        }

        try (FileInputStream fis = openFileInputStream(srcFile);
             FileOutputStream fos = openFileOutputStream(destFile)) {
            final FileChannel input = fis.getChannel();
            final FileChannel output = fos.getChannel();

            final long size = input.size();
            long pos = 0;
            long count = 0;

            while (pos < size) {
                count = ((size - pos) > FILE_COPY_BUFFER_SIZE) ? FILE_COPY_BUFFER_SIZE : (size - pos);
                final long transferred = output.transferFrom(input, pos, count);

                if (transferred > 0) {
                    pos += transferred;
                    continue;
                }

                // transferFrom is permitted to make no progress on some platforms. Fall back to a
                // regular channel read/write so a transient zero cannot turn this into an infinite loop.
                final ByteBuffer fallbackBuffer = ByteBuffer.allocate((int) Math.min(8192L, size - pos));
                input.position(pos);
                final int bytesRead = input.read(fallbackBuffer);

                if (bytesRead < 0) {
                    break;
                } else if (bytesRead == 0) {
                    throw new IOException("Unable to make progress while copying '" + describe(srcFile) + "' to '" + describe(destFile) + "'");
                }

                fallbackBuffer.flip();
                output.position(pos);

                while (fallbackBuffer.hasRemaining()) {
                    if (output.write(fallbackBuffer) <= 0) {
                        throw new IOException("Unable to make progress while writing '" + describe(destFile) + "'");
                    }
                }

                pos += bytesRead;
            }
        }

        if (srcFile.length() != destFile.length()) {
            deleteIfExists(destFile);
            throw new IOException("Failed to copy full contents from '" + describe(srcFile) + "' to '" + describe(destFile) + "'");
        }

        // Both callers hand in a resolved source (copyToDirectory canonicalises it, doCopyDirectory routes links
        // to copySymbolicLink first), so no link test is needed here.
        if (preserveFileDate && !setTimes(srcFile, destFile)) {
            throw new IOException("Cannot set the file time for '" + describe(destFile) + "' (copied from '" + describe(srcFile) + "')");
        }
    }

    /**
     * Throws IllegalArgumentException if the two files denote the same on-disk file.
     *
     * <p>This rejects both the trivial case (equal canonical paths) and the case where two distinct
     * directory entries resolve to the same file via a hard link (same inode/file key, different path).
     * The latter is only detectable when both files actually exist, so {@link Files#isSameFile(Path, Path)}
     * is consulted only in that case.
     *
     * @param file1 the first file to compare.
     * @param file2 the second file to compare.
     * @throws IllegalArgumentException if the two files denote the same on-disk file.
     * @throws IOException if resolving or comparing the canonical paths of {@code file1} and {@code file2} fails
     */
    private static void requireCanonicalPathsNotEquals(final File file1, final File file2) throws IllegalArgumentException, IOException {
        final String canonicalPath = file1.getCanonicalPath();
        if (canonicalPath.equals(file2.getCanonicalPath())) {
            throw new IllegalArgumentException(
                    String.format("File canonical paths are equal: '%s' (file1='%s', file2='%s')", canonicalPath, describe(file1), describe(file2)));
        }

        // Distinct canonical paths can still point to the same underlying file through a hard link
        // (same inode, different directory entry). Files.isSameFile compares file keys and detects that,
        // but requires both files to exist, so guard the call.
        if (file1.exists() && file2.exists() && Files.isSameFile(file1.toPath(), file2.toPath())) {
            throw new IllegalArgumentException(
                    String.format("Files denote the same on-disk file (hard-link alias): file1='%s', file2='%s'", describe(file1), describe(file2)));
        }
    }

    /**
     * The directory's location for a containment check: the real path when it can be resolved, so a
     * Windows junction or a symbolic link is judged by where writes actually go, and the canonical
     * path when it has not been created yet (the spelling under which it would be).
     *
     * <p>{@link File#getCanonicalPath()} does not follow a Windows directory junction, which made a
     * destination that is a junction inside the source pointing outside look like a copy into the
     * source, and a junction outside pointing back in look like a legal sibling.
     *
     * @param dir the directory whose location is needed; must not be {@code null}.
     * @return the real path, or the canonical path if the real path cannot be resolved.
     * @throws IOException if the canonical path cannot be obtained either.
     */
    private static Path resolvedDirectoryLocation(final File dir) throws IOException {
        try {
            return dir.toPath().toRealPath();
        } catch (final IOException e) {
            return dir.getCanonicalFile().toPath();
        }
    }

    /**
     * Rejects a destination directory that is the source directory itself, or lies inside it.
     *
     * <p>Copying or moving a directory into itself has no meaning, and attempting it damages the source: the
     * walk keeps finding the entries that were just written. Every operation that takes a source directory and
     * a destination directory routes through this check so they all report the same
     * {@link IllegalArgumentException} for the same input, rather than a platform-specific
     * {@code FileSystemException} raised half-way through.
     *
     * <p>The opposite direction - a source that lies inside the destination - is legal and is not rejected.
     *
     * <p>Callers pass <i>resolved</i> locations ({@link Path#toRealPath(java.nio.file.LinkOption[])} when the directory exists), not
     * merely {@link File#getCanonicalPath()}: on Windows the latter does not follow a directory junction.
     *
     * @param srcCanonicalPath the resolved path of the source directory.
     * @param destCanonicalPath the resolved path of the destination directory.
     * @param operation the verb used in the message ({@code "copy"} / {@code "move"}).
     * @throws IllegalArgumentException if the destination is the source or is inside it.
     */
    private static void requireDestDirectoryOutsideSourceDirectory(final String srcCanonicalPath, final String destCanonicalPath, final String operation)
            throws IllegalArgumentException {
        if (isSameOrInside(destCanonicalPath, srcCanonicalPath)) {
            throw new IllegalArgumentException("Failed to " + operation + " due to the target directory: " + destCanonicalPath
                    + " is in or same as the source directory: " + srcCanonicalPath);
        }
    }

    /**
     * Rejects a copy whose <i>target</i> - the directory {@code destDir/srcDir.getName()} that the source is
     * recreated as - would be the source directory itself or one of its ancestors.
     *
     * <p>This is the other half of {@link #requireDestDirectoryOutsideSourceDirectory(String, String, String)}.
     * A {@code destDir} that is an ancestor of the source is legal and stays legal, but the target <i>inside</i>
     * it must still be a place of its own. The two coincide whenever a name repeats along the path: with
     * {@code destDir} the source's own parent the target is the source, and copying {@code "g/b/c/b"} into
     * {@code "g"} aims it at {@code "g/b"}, an ancestor. {@code doCopyDirectory} then walked the source onto
     * itself - copying its entries INTO the source tree and returning normally, or, for a source with nothing
     * to copy, reporting success having done nothing and handing back the source as "the copy" (a caller that
     * then deleted the source, the idiom {@code moveToDirectory}'s own javadoc suggests, lost the tree).
     *
     * @param srcCanonicalPath the canonical path of the source directory.
     * @param targetCanonicalPath the canonical path of the directory the source would be recreated as.
     * @throws IllegalArgumentException if the target is the source directory or one of its ancestors.
     */
    private static void requireCopyTargetOutsideSourceDirectory(final String srcCanonicalPath, final String targetCanonicalPath)
            throws IllegalArgumentException {
        if (isSameOrInside(srcCanonicalPath, targetCanonicalPath)) {
            throw new IllegalArgumentException("Failed to copy due to the target directory: " + targetCanonicalPath
                    + " is the source directory, or one of its ancestors: " + srcCanonicalPath);
        }
    }

    /**
     * Returns whether the canonical path {@code inner} denotes {@code outer} itself or something inside it.
     *
     * <p>A prefix match alone is not enough: {@code "/a/bc"} starts with {@code "/a/b"} without being inside
     * it, so the character right after the prefix has to be a separator. A filesystem root is the exception,
     * and used to be a hole: its canonical path already <i>ends</i> with the separator ({@code "C:\"},
     * {@code "/"}), so the character after the prefix is the first character of a name and the test answered
     * {@code false} for every path on that volume - leaving {@code copyToDirectory}, {@code copyDirectory} and
     * {@code moveToDirectory} unguarded for a root source.
     *
     * @param inner the canonical path being tested.
     * @param outer the canonical path it may sit inside.
     * @return {@code true} if {@code inner} is {@code outer} or lies inside it.
     */
    private static boolean isSameOrInside(final String inner, final String outer) {
        if (!inner.startsWith(outer)) {
            return false;
        }

        if (inner.length() == outer.length() || endsWithSeparator(outer)) {
            return true;
        }

        final char next = inner.charAt(outer.length());

        return next == '/' || next == '\\';
    }

    private static boolean endsWithSeparator(final String path) {
        if (path.isEmpty()) {
            return false;
        }

        final char last = path.charAt(path.length() - 1);

        return last == '/' || last == '\\';
    }

    /**
     * Creates all parent directories for a File object, including any necessary but non-existent parent directories. If a parent directory already exists or
     * is {@code null}, nothing happens.
     *
     * <p>The {@code false} answer must not be discarded: a caller that ignores it leaves an uncreatable parent
     * to surface as whatever its next call happens to raise - a bare {@code NoSuchFileException} from
     * {@link Files#copy(Path, Path, CopyOption...)}, naming the destination but not the reason - while the same
     * environment failure reached through {@code createNewFileIfNotExists} already reports which directory could
     * not be made.
     *
     * @param file the File that may need parents, which may be {@code null}.
     * @return {@code true} if the parent directory exists or was successfully created; {@code false} otherwise.
     */
    private static boolean createParentDirectories(final File file) {
        final File parent = getParentFile(file);

        // isDirectory() is re-tested after mkdirs(): a concurrent creator makes mkdirs() answer false for a
        // directory that does now exist, which is success rather than failure.
        return parent == null || parent.isDirectory() || parent.mkdirs() || parent.isDirectory();
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
     * Sets the {@code lastModifiedTime}, {@code lastAccessTime} and {@code creationTime} of the target file
     * to match those of the source file.
     *
     * @param sourceFile the source file to query.
     * @param targetFile the target file or directory to set.
     * @return {@code true} if and only if the operation succeeded; {@code false} otherwise.
     * @throws IllegalArgumentException if {@code sourceFile} or {@code targetFile} is {@code null}
     */
    private static boolean setTimes(final File sourceFile, final File targetFile) throws IllegalArgumentException {
        N.checkArgNotNull(sourceFile, cs.sourceFile);
        N.checkArgNotNull(targetFile, cs.targetFile);

        try {
            // Set creation, modified, last accessed to match source file
            final BasicFileAttributes srcAttr = Files.readAttributes(sourceFile.toPath(), BasicFileAttributes.class);
            final BasicFileAttributeView destAttrView = Files.getFileAttributeView(targetFile.toPath(), BasicFileAttributeView.class);
            // setTimes(..) accepts a null FileTime for any of the three, so the attributes need no guarding.
            // (destAttrView itself is not guarded: BasicFileAttributeView is supported by every provider, and a
            // provider that somehow returned null would surface as an NPE from here rather than be swallowed.)
            destAttrView.setTimes(srcAttr.lastModifiedTime(), srcAttr.lastAccessTime(), srcAttr.creationTime());
            return true;
        } catch (final IOException ignored) {
            // Fallback: Only set modified time to match source file
            return targetFile.setLastModified(sourceFile.lastModified());
        }
    }

    /**
     * Copies the contents of a directory to another directory.
     * <p>
     * This method copies all files and subdirectories from the source directory to the destination directory.
     * If the destination directory does not exist, it is created. Immediate children that are symbolic
     * links are copied as links and are not followed; a Windows junction is followed and copied as a plain
     * directory, as {@link #copyToDirectory(File, File)} copies one, except that a dangling junction, a junction
     * leading back to a directory on the path being copied or reaching {@code destDir} (or a directory above or
     * inside it), and a special file (FIFO, socket, device node) are left out; any other junction is followed, so
     * its target may be copied more than once. Existing files in the destination are never overwritten: an {@code IOException} is thrown for the
     * first one met, and the entries copied before it stay in place.
     * </p>
     *
     * <p>{@code destDir} must not be {@code srcDir} itself, nor lie inside it: the copy would keep finding the
     * entries it had just written. That is rejected before anything is created, so no partial copy is left in
     * the source. The opposite direction - copying a directory into one of its own ancestors - is legal, with
     * one exception: a subdirectory whose name repeats a directory already on the path from {@code destDir}
     * down to {@code srcDir} would be copied straight onto that ancestor, so it is rejected. Copying
     * {@code "a/b"} into {@code "a"} therefore works, unless {@code "a/b"} itself holds a directory called
     * {@code "b"}.</p>
     *
     * <p>Like every mutating operation in this class, a copy is not transactional: a failure part-way through
     * leaves whatever was already copied in {@code destDir}.</p>
     *
     * <p><b>Timestamps are preserved</b>, exactly as {@link #copyToDirectory(File, File)} preserves them for each
     * entry: every copied file carries its source's times, a copied subdirectory does on a best-effort basis (a
     * failure to date a directory is not reported), and the copy fails - after the bytes
     * have been written - if a file's time cannot be applied. {@code destDir} itself keeps its own time. There is
     * no switch on this method; copy the entries with {@link #copyToDirectory(File, File, boolean)} and
     * {@code false} when the dates must not be carried over.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File sourceDir = new File("source_directory");
     * File destDir = new File("destination_directory");
     * IOUtil.copyDirectory(sourceDir, destDir);
     * }</pre>
     *
     * @param srcDir  the source directory to copy from, must not be {@code null}; it must exist and be readable.
     * @param destDir the destination directory to copy to, must not be {@code null}.
     * @throws IllegalArgumentException if {@code srcDir} is {@code null} or exists but is not a directory, if
     *         {@code destDir} is {@code null} or exists but is not a directory, if {@code destDir} is
     *         {@code srcDir} or is inside it, or if one of {@code srcDir}'s subdirectories would be copied
     *         onto one of its own ancestors.
     * @throws IOException if listing or reading {@code srcDir} , creating {@code destDir} or its descendants, or copying their contents fails,
     *         if the source directory does not exist or cannot be read (a {@link FileNotFoundException} ), if its contents cannot be listed
     *         &mdash; which is reported rather than mistaken for an empty directory, so a normal return always means the whole source was copied
     *         &mdash; or if a copied file's timestamp cannot be applied.
     * @see #copyToDirectory(File, File)
     */
    public static void copyDirectory(final File srcDir, final File destDir) throws IllegalArgumentException, IOException {
        checkDirectoryExists(srcDir, cs.srcDir);
        // Validate only; the destination is created below, after the containment check has passed, so a
        // rejected call cannot leave a freshly created directory behind - possibly inside the source tree.
        checkDestDirectory(destDir);

        // A destination that IS the source, or sits inside it, used to be discovered only once the walk
        // reached it: by then part of the tree had already been copied INTO the source ("Copy of x.txt"
        // entries appeared in srcDir), how far it got depended on File.listFiles() ordering, and the failure
        // named the recursive call's arguments rather than the caller's. The peer copyToDirectory(..) has
        // always rejected the same input up front; this makes the two agree.
        requireDestDirectoryOutsideSourceDirectory(resolvedDirectoryLocation(srcDir).toString(), resolvedDirectoryLocation(destDir).toString(), "copy");

        createDestDirectory(destDir);

        // One cycle guard for the whole walk, seeded with this directory and the destination: an immediate child
        // that is a junction to destDir (or to a directory above or inside it) is left out here rather than
        // rejected by the child's own containment check after its siblings were copied, and a junction deeper
        // down that leads back to srcDir is left out as it is under copyToDirectory. Canonicalised first, as
        // copyToDirectory canonicalises its arguments, and the destination only now that it exists: toRealPath()
        // keeps a subst drive alias ("Q:\src", no reparse point on the path) while a junction's real target is
        // always spelled on the real volume, so a guard seeded from the alias could never match and a junction to
        // the destination ran away again through such a spelling (see CopyCycleGuard for the JDK-21 case).
        final CopyCycleGuard guard = new CopyCycleGuard(srcDir.getCanonicalFile().toPath().toRealPath());
        guard.outputRoot = destDir.getCanonicalFile().toPath().toRealPath();

        // Listed directly rather than through listFiles(File): that one folds a null listing into an empty
        // result, which is right for a walk (an unreadable subdirectory contributes nothing) but wrong here -
        // reporting a successful copy of a source whose contents could not be read is how a caller following
        // the documented copy-then-delete idiom loses the original. doCopyDirectory applies the same guard.
        final File[] files = srcDir.listFiles();

        if (files == null) {
            throw new IOException("Failed to list contents of " + describe(srcDir));
        }

        for (final File file : files) {
            if (file == null) {
                continue;
            }

            // Immediate children are nested relative to srcDir: copy links as links, matching
            // doCopyDirectory. copyToDirectory would canonicalize a directory symlink and follow it.
            if (Files.isSymbolicLink(file.toPath())) {
                copySymbolicLink(file, new File(destDir, file.getName()));
            } else if (!isLeftOutOfCopy(file, guard)) {
                // A dangling junction, a junction back into the copy, or a special file is left out here
                // exactly as doCopyDirectory leaves it out of a nested directory; copyToDirectory would reject
                // it as a bad argument (or, for a FIFO, block for ever reading it).
                copyToDirectory(file, destDir, true, BiPredicates.alwaysTrue(), guard);
            }
        }
    }

    /**
     * Copies a file to a new location preserving the file date.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, then this method
     * overwrites it. A symbolic link is resolved before copying, so the new file is not a link. The copy is made
     * with {@link Files#copy(Path, Path, CopyOption...)}, which carries the source's POSIX permission bits, or
     * Windows attributes such as read-only, to the new file - the directory copies ({@code copyToDirectory},
     * {@code copyDirectory}) write through streams and give their copies default permissions. Whether an
     * existing read-only destination is replaced is platform-defined: POSIX replaces the entry when its directory
     * is writable, Windows refuses with an {@code AccessDeniedException}.
     * </p>
     * <p>
     * <strong>Note:</strong> This method tries to preserve the file's last modified date/times using
     * {@link BasicFileAttributeView#setTimes(FileTime, FileTime, FileTime)}. However, it is not guaranteed that the
     * operation will succeed. If the modification operation fails, it falls back to
     * {@link File#setLastModified(long)}, and if that fails, the method throws IOException.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("original.txt");
     * File dest = new File("copy.txt");
     * IOUtil.copyFile(source, dest);
     * }</pre>
     *
     * <p><b>{@code copyFile} or {@code write}?</b> This method preserves the source's timestamps (failing if it
     * cannot), which is what makes it a <i>copy</i>. {@link #write(File, File)} streams the same bytes but leaves
     * the destination with a fresh modification time and returns the byte count; it also has a sliced
     * {@link #write(File, long, long, File)} form that {@code copyFile} does not.
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destFile the new file, must not be {@code null}.
     * @throws IllegalArgumentException if {@code srcFile} is {@code null} or is a directory, or if {@code srcFile}
     *         and {@code destFile} denote the same file.
     * @throws FileNotFoundException if the source does not exist, is not readable, or is neither a file nor a
     *         directory (a FIFO, socket or device node, or a link to one).
     * @throws IOException if source or destination is invalid, or if reading {@code srcFile} or writing {@code destFile} fails.
     * @see #copyToDirectory(File, File)
     * @see #copyFile(File, File, boolean)
     * @see #write(File, File)
     */
    public static void copyFile(final File srcFile, final File destFile) throws IllegalArgumentException, IOException {
        copyFile(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies an existing file to a new file location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, then this method
     * overwrites it. A symbolic link is resolved before copying so the new file is not a link. The copy is made
     * with {@link Files#copy(Path, Path, CopyOption...)}, which carries the source's POSIX permission bits, or
     * Windows attributes such as read-only, to the new file - the directory copies ({@code copyToDirectory},
     * {@code copyDirectory}) write through streams and give their copies default permissions. Whether an
     * existing read-only destination is replaced is platform-defined: POSIX replaces the entry when its directory
     * is writable, Windows refuses with an {@code AccessDeniedException}.
     * </p>
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the file's last
     * modified date/times using {@link BasicFileAttributeView#setTimes(FileTime, FileTime, FileTime)}. However, it is
     * not guaranteed that the operation will succeed. If the modification operation fails, it falls back to
     * {@link File#setLastModified(long)}, and if that fails, the method throws IOException.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("original.txt");
     * File dest = new File("copy.txt");
     * IOUtil.copyFile(source, dest, true);
     * }</pre>
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destFile the new file, must not be {@code null}.
     * @param preserveFileDate {@code true} if the file date of the copy should be the same as the original.
     * @throws IllegalArgumentException if {@code srcFile} is {@code null} or is a directory, or if {@code srcFile}
     *         and {@code destFile} denote the same file.
     * @throws FileNotFoundException if the source does not exist, is not readable, or is neither a file nor a
     *         directory (a FIFO, socket or device node, or a link to one).
     * @throws IOException if source or destination is invalid, if reading {@code srcFile} or writing {@code destFile} fails, if setting the
     *         last-modified time didn't succeed, or if the output file length differs from the input after copying.
     * @see #copyFile(File, File, boolean, CopyOption...)
     */
    public static void copyFile(final File srcFile, final File destFile, final boolean preserveFileDate) throws IllegalArgumentException, IOException {
        copyFile(srcFile, destFile, preserveFileDate, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies a file to a new location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, you can overwrite
     * it if you use {@link StandardCopyOption#REPLACE_EXISTING}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("original.txt");
     * File dest = new File("copy.txt");
     * IOUtil.copyFile(source, dest, StandardCopyOption.REPLACE_EXISTING);
     * }</pre>
     *
     * <p><strong>This overload preserves the source's file dates</strong>, exactly as
     * {@link #copyFile(File, File)} does: it delegates with {@code preserveFileDate = true}. That also means it
     * can fail after the bytes have been copied, if the timestamps cannot be applied. Use
     * {@link #copyFile(File, File, boolean, CopyOption...)} with {@code false} to opt out.
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destFile the new file, must not be {@code null}.
     * @param copyOptions options specifying how the copy should be done, for example {@link StandardCopyOption}.
     *                    Must not be {@code null}; pass no arguments, or an empty array, for "no options".
     * @throws IllegalArgumentException if {@code srcFile}, {@code destFile} or {@code copyOptions} is
     *         {@code null}, if {@code srcFile} is a directory, or if {@code srcFile} and {@code destFile} denote
     *         the same file.
     * @throws FileNotFoundException if the source does not exist, is not readable, or is neither a file nor a
     *         directory (a FIFO, socket or device node, or a link to one).
     * @throws IOException if reading {@code srcFile} or writing {@code destFile} fails, or if setting the last-modified time didn't succeed.
     * @see #copyFile(File, File, boolean, CopyOption...)
     * @see StandardCopyOption
     */
    @SafeVarargs
    public static void copyFile(final File srcFile, final File destFile, final CopyOption... copyOptions) throws IllegalArgumentException, IOException {
        copyFile(srcFile, destFile, true, copyOptions);
    }

    /**
     * Copies the contents of a file to a new location.
     * <p>
     * This method copies the contents of the specified source file to the specified destination file. The directory
     * holding the destination file is created if it does not exist. If the destination file exists, you can overwrite
     * it with {@link StandardCopyOption#REPLACE_EXISTING}.
     * </p>
     *
     * <p>
     * By default, a symbolic link is resolved before copying so the new file is not a link.
     * To copy symbolic links as links, you can pass {@link LinkOption#NOFOLLOW_LINKS} as the last argument. The
     * link itself is then what has to exist: a dangling link, or a link to a directory, is copied as-is, and no
     * date is applied to the new link.
     * </p>
     *
     * <p>
     * <strong>Note:</strong> Setting {@code preserveFileDate} to {@code true} tries to preserve the file's last
     * modified date/times using {@link BasicFileAttributeView#setTimes(FileTime, FileTime, FileTime)}. However, it is
     * not guaranteed that the operation will succeed. If the modification operation fails, it falls back to
     * {@link File#setLastModified(long)}, and if that fails, the method throws IOException.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("original.txt");
     * File dest = new File("copy.txt");
     * IOUtil.copyFile(source, dest, true, StandardCopyOption.REPLACE_EXISTING);
     * }</pre>
     *
     * @param srcFile an existing file to copy, must not be {@code null}.
     * @param destFile the new file, must not be {@code null}.
     * @param preserveFileDate {@code true} if the file date of the copy should be the same as the original.
     * @param copyOptions options specifying how the copy should be done, for example {@link StandardCopyOption}.
     *                    Must not be {@code null}; pass no arguments, or an empty array, for "no options".
     * @throws IllegalArgumentException if {@code srcFile}, {@code destFile} or {@code copyOptions} is
     *         {@code null}, if {@code srcFile} is not a file (unless it is a link copied as a link), if
     *         {@code destFile} is a directory, or if they denote the same file.
     * @throws FileNotFoundException if the source does not exist or is not readable (for a link copied as a link,
     *         if the link itself does not exist).
     * @throws IOException if reading {@code srcFile} or writing {@code destFile} fails, if setting the last-modified time didn't succeed, or if
     *         the destination is not writable.
     * @see #copyToDirectory(File, File, boolean)
     */
    @SafeVarargs
    public static void copyFile(final File srcFile, final File destFile, final boolean preserveFileDate, final CopyOption... copyOptions)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(srcFile, cs.srcFile);
        N.checkArgNotNull(destFile, cs.destFile);
        N.checkArgNotNull(copyOptions, cs.copyOptions);

        // A link copied AS a link only has to exist as a link. checkFileExists(..) follows it, so a dangling link
        // read as "does not exist" and a link to a directory as "is not a file" - the two links NOFOLLOW_LINKS
        // exists to copy, and the option never got as far as Files.copy(..). Every other source, a followed link
        // included, is validated as before.
        if (!(N.contains(copyOptions, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(srcFile.toPath()))) {
            checkFileExists(srcFile, cs.srcFile);
        }

        requireCanonicalPathsNotEquals(srcFile, destFile);

        // The result used to be discarded, so a parent directory that could not be created surfaced as a bare
        // NoSuchFileException from Files.copy(..) - naming the destination but not the reason - while the same
        // failure through openFileOutputStream(..) already said which directory was at fault.
        if (!createParentDirectories(destFile)) {
            throw new IOException("Failed to create parent directory: " + describe(getParentFile(destFile)));
        }

        // Only the wrong KIND is rejected here. The destination used to go through checkFileExists(..), the
        // SOURCE validator, which also demanded that it be readable - so an existing write-only destination was
        // refused with "exists but cannot be read", a complaint about a permission a destination does not need.
        if (destFile.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(destFile) + "' is a directory, not a file");
        }

        final Path srcPath = srcFile.toPath();
        final Path destPath = destFile.toPath();

        Files.copy(srcPath, destPath, copyOptions);

        // The guard is on the DESTINATION being a link (NOFOLLOW_LINKS copied the link itself, and stamping a link
        // would reach through it to the target), not on the source being one: a link source that was followed has
        // produced an ordinary file, whose date this method promises to preserve. Testing the source instead
        // skipped the stamp for every link source - silently, on Unix, where Files.copy does not carry the time
        // over by itself. (On Windows CopyFileEx copies the write time anyway, which is what hid it.)
        if (preserveFileDate && !Files.isSymbolicLink(destPath) && !setTimes(srcFile, destFile)) {
            throw new IOException("Cannot set the file time for '" + describe(destFile) + "' (copied from '" + describe(srcFile) + "')");
        }
    }

    /**
     * Copies bytes from a {@link File} to an {@link OutputStream}.
     * <p>
     * This method buffers the input internally, so there is no need to use a {@link BufferedInputStream}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File source = new File("data.txt");
     * try (OutputStream out = new FileOutputStream("output.bin")) {
     *     long bytesCopied = IOUtil.copyFile(source, out);
     * }
     * }</pre>
     *
     * <p>This is an alias for {@link #write(File, OutputStream)}, which is the same operation named for the
     * {@code write} family. Use whichever reads better at the call site; prefer
     * {@link #write(File, long, long, OutputStream)} when only a slice of the file is wanted.
     *
     * @param srcFile the {@link File} to read.
     * @param output  the {@link OutputStream} to write.
     * @return the number of bytes copied.
     * @throws IllegalArgumentException if {@code srcFile} or {@code output} is {@code null}.
     * @throws FileNotFoundException if {@code srcFile} does not exist or is not readable.
     * @throws IOException if reading {@code srcFile} or writing {@code output} fails
     * @see #write(File, OutputStream)
     * @see #copyFile(File, File)
     */
    public static long copyFile(final File srcFile, final OutputStream output) throws IllegalArgumentException, IOException {
        // Delegating rather than calling Files.copy(Path, OutputStream): the javadoc calls this an alias of
        // write(File, OutputStream), and the two used to disagree on their failure mode - a missing source gave
        // FileNotFoundException here and NoSuchFileException there, so a catch written against one twin missed
        // the other.
        return write(srcFile, output);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url = new URL("https://example.com/data.txt");
     * File dest = new File("downloaded.txt");
     * IOUtil.copyURLToFile(url, dest);
     * }</pre>
     *
     * <p>A {@code file:} URL that names {@code destination} itself is rejected as a bad argument. A URL spelling
     * this class cannot map to a local {@code File} is not checked; see the note on
     * {@link #copyURLToFile(URL, File, int, int)}.
     *
     * <p><b>The previous content survives a failed transfer.</b> The bytes are written to a temporary sibling
     * of {@code destination} and moved into place only once the whole stream has arrived, so a dropped
     * connection part-way through leaves {@code destination} exactly as it was rather than truncated and
     * half-overwritten. The move replaces the destination <i>entry</i>: a symbolic link at {@code destination}
     * is replaced by the downloaded file, not written through - except where no replacing move is possible (a
     * Windows file another process holds open, or a directory that will not take a new entry), in which case the
     * complete download is written in place, as this method always used to write. The destination keeps its
     * POSIX permissions either way. Whether a read-only destination is replaced is platform-defined: the
     * replacing move succeeds on POSIX when the directory is writable, and Windows refuses it. A leftover
     * temporary file is removed on failure. In the in-place case it may also survive a <i>successful</i> call:
     * the destination already holds the whole download, so a sibling that cannot be removed afterwards is
     * logged rather than reported, leaving a {@code <destination name>.<hex>.part} file next to the destination.
     *
     * @param source      the {@code URL} to copy bytes from, must not be {@code null}.
     * @param destination the non-directory {@code File} to write bytes to
     *                    (possibly overwriting), must not be {@code null}.
     * @throws IllegalArgumentException if {@code source} or {@code destination} is {@code null}, if
     *         {@code destination} is a directory, or if {@code source} is a {@code file:} URL naming
     *         {@code destination} itself.
     * @throws IOException if opening or reading {@code source} or writing or installing the downloaded file at {@code destination} fails.
     */
    public static void copyURLToFile(final URL source, final File destination) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(destination, cs.destination);

        requireSourceUrlIsNotTheDestination(source, destination);
        requireNotDirectory(destination, "destination");

        try (InputStream is = source.openStream()) {
            writeThroughTempFile(is, destination);
        }
    }

    /**
     * Copies bytes from the URL {@code source} to a file
     * {@code destination}. The directories up to {@code destination}
     * will be created if they don't already exist. {@code destination}
     * will be overwritten if it already exists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url = new URL("https://example.com/data.txt");
     * File dest = new File("downloaded.txt");
     * IOUtil.copyURLToFile(url, dest, 5000, 10000);
     * }</pre>
     *
     * <p>As with {@link #copyURLToFile(URL, File)}, a {@code file:} URL naming {@code destination} itself is
     * rejected, and the previous content of {@code destination} survives a transfer that fails part-way (a read
     * timeout included): the bytes land in a temporary sibling that replaces the destination entry only once the
     * whole stream has arrived - including the case where no replacing move is possible, the download is written
     * in place instead, and the sibling that could not be removed afterwards survives the successful call. The
     * self-copy check is deliberately conservative: only a
     * {@code file:} URL with a local authority is resolved, and a spelling this class cannot map to a
     * {@code File} - a percent-encoded path separator, or the legacy {@code "file:/C|/.."} drive form, whose
     * canonical path cannot be resolved - names nothing to compare and is left exactly as it was rather than
     * being rejected for a second reason.
     *
     * @param source            the {@code URL} to copy bytes from, must not be {@code null}.
     * @param destination       the non-directory {@code File} to write bytes to
     *                          (possibly overwriting), must not be {@code null}.
     * @param connectTimeout    the number of milliseconds until this method
     *                          will timeout if no connection could be established to the {@code source}.
     * @param readTimeout       the number of milliseconds until this method will
     *                          timeout if no data could be read from the {@code source}.
     * @throws IllegalArgumentException if {@code source} or {@code destination} is {@code null}, if
     *         {@code destination} is a directory, or if {@code source} is a {@code file:} URL naming
     *         {@code destination} itself.
     * @throws IOException if opening or reading {@code source} or writing or installing the downloaded file at {@code destination} fails.
     */
    public static void copyURLToFile(final URL source, final File destination, final int connectTimeout, final int readTimeout)
            throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(destination, cs.destination);

        requireSourceUrlIsNotTheDestination(source, destination);
        requireNotDirectory(destination, "destination");

        final URLConnection connection = source.openConnection();
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        try (InputStream is = connection.getInputStream()) {
            writeThroughTempFile(is, destination);
        } finally {
            close(connection);
        }
    }

    /**
     * Rejects a destination that exists and is a directory, as a wrong-kind argument.
     *
     * <p>{@code copyURLToFile} used to discover this inside {@code write(is, destination)}, which classified the
     * failed open. Now that the download lands in a temporary file that is <i>moved</i> over the destination, the
     * check has to be explicit: {@link Files#move(Path, Path, CopyOption...)} with {@code REPLACE_EXISTING} would
     * happily replace an <i>empty</i> directory with the file.
     *
     * @param file the destination to test.
     * @param argName the caller's name for it.
     * @throws IllegalArgumentException if {@code file} is a directory.
     */
    private static void requireNotDirectory(final File file, final String argName) throws IllegalArgumentException {
        if (file.isDirectory()) {
            throw new IllegalArgumentException("'" + argName + "' is a directory, not a file: " + describe(file));
        }
    }

    /**
     * Writes {@code is} to a temporary sibling of {@code destination} and moves it into place once the whole
     * stream has arrived, so that a transfer which fails part-way - a dropped connection, a read timeout - leaves
     * the previous {@code destination} untouched instead of truncated and half-overwritten, which is what
     * {@code write(is, destination)} did: it opened, and so truncated, the destination before the first byte had
     * been read.
     *
     * <p>The sibling lives in the destination's own directory (created here if missing, as every other
     * file-creating method creates it), so the final move never crosses a filesystem. The move is atomic where the
     * platform allows and falls back to a plain replace where it does not. On any failure the sibling is removed
     * and the original failure is rethrown, with a failed removal attached as a suppressed exception.
     *
     * @param is the stream to write; not closed here.
     * @param destination the file to replace.
     * @throws IOException if the parent directory cannot be created, the stream cannot be read, the sibling cannot
     *         be written, or the move fails.
     */
    private static void writeThroughTempFile(final InputStream is, final File destination) throws IOException {
        if (!createParentDirectories(destination)) {
            throw new IOException("Failed to create parent directory: " + describe(getParentFile(destination)));
        }

        final File target = destination.getAbsoluteFile();
        final File part = newPartFile(target);

        if (part == null) {
            // The directory does not allow a new entry (unwritable, or a sticky /tmp-style directory owned by
            // someone else) although the file itself may be writable - the shape the old direct overwrite
            // handled. Keep handling it, at the cost of the guarantee above for this call only.
            write(is, target);

            return;
        }

        try {
            write(is, part);
            copyPosixPermissions(target, part);
            replaceWith(part, target);
        } catch (final Throwable e) {
            try {
                Files.deleteIfExists(part.toPath());
            } catch (final IOException suppressed) {
                e.addSuppressed(suppressed);
            }

            throw e;
        }
    }

    /**
     * Creates an empty, uniquely named sibling of {@code target} to download into, or answers {@code null} when the
     * directory will not take a new entry.
     *
     * <p>Not {@link Files#createTempFile(Path, String, String, java.nio.file.attribute.FileAttribute...)}: on POSIX
     * that creates the file {@code rw-------}, and the mode travels with the file when it is moved over the
     * destination, so a download quietly turned a {@code rw-r--r--} file into one its readers could no longer
     * open. {@link File#createNewFile()} applies the process's umask like every other file this class creates.
     *
     * @param target the absolute destination.
     * @return the created sibling, or {@code null} if none could be created.
     */
    @MayReturnNull
    private static File newPartFile(final File target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            final File part = new File(target.getParentFile(),
                    target.getName() + "." + Long.toHexString(System.nanoTime() ^ Thread.currentThread().threadId()) + ".part");

            try {
                if (part.createNewFile()) {
                    return part;
                }
            } catch (final IOException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Gives {@code part} the POSIX permissions {@code target} already has, so that replacing the destination
     * does not change its mode; a no-op where {@code target} does not exist or the file system has no POSIX
     * attributes. A failure here is not worth failing the download over, so it is only logged.
     */
    private static void copyPosixPermissions(final File target, final File part) {
        if (!target.exists()) {
            return;
        }

        try {
            final java.nio.file.attribute.PosixFileAttributeView view = Files.getFileAttributeView(target.toPath(),
                    java.nio.file.attribute.PosixFileAttributeView.class);

            if (view != null) {
                Files.setPosixFilePermissions(part.toPath(), view.readAttributes().permissions());
            }
        } catch (final IOException | UnsupportedOperationException e) {
            logger.warn(e, "Could not carry the permissions of {} over to the downloaded file", target);
        }
    }

    /**
     * Puts {@code part} in place of {@code target}: an atomic move where the platform allows it, a plain replacing
     * move where it does not, and - when neither move is possible - the content is written over the destination
     * in place, which is what a Windows file held open by a reader needs: a move cannot replace it (the reader's
     * handle lacks {@code FILE_SHARE_DELETE}) but a truncating write can, exactly as the old direct overwrite did.
     * The in-place fallback runs only once the whole download has arrived, so the guarantee that a failed
     * transfer never truncates the destination still holds; only a local write failure can leave it partial.
     *
     * <p>A temporary sibling that cannot be removed after a successful in-place write is left on disk and
     * logged rather than reported: the destination already holds the whole download, so failing the call would
     * describe a transfer that in fact succeeded.</p>
     *
     * @param part the fully written sibling.
     * @param target the destination to replace.
     * @throws IOException if none of the three ways succeeds (the in-place write's failure, with the move's
     *         failure attached as a suppressed exception).
     */
    private static void replaceWith(final File part, final File target) throws IOException {
        final Path from = part.toPath();
        final Path to = target.toPath();
        IOException moveFailure;

        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            return;
        } catch (final AtomicMoveNotSupportedException e) {
            try {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);

                return;
            } catch (final IOException plainMoveFailure) {
                moveFailure = plainMoveFailure;
            }
        } catch (final IOException e) {
            moveFailure = e;
        }

        try {
            write(part, target);
        } catch (final IOException e) {
            e.addSuppressed(moveFailure);

            throw e;
        }

        try {
            Files.deleteIfExists(from);
        } catch (final IOException e) {
            // The destination already holds the whole download, so the operation succeeded; turning a failed
            // cleanup of the temporary sibling into a thrown IOException would tell the caller the transfer
            // failed when it did not, and a caller that then retries or rolls back is acting on a wrong answer.
            // Best-effort-and-log, as copyPosixPermissions and applyEntryTimeQuietly already are.
            logger.warn(e, "Could not remove the temporary download file {} after writing {} in place", from, to);
        }
    }

    /**
     * Rejects a {@code file:} URL that names {@code destination} itself.
     *
     * <p>{@code write(is, destination)} opens - and therefore truncates - the destination before the first
     * byte has been read from the URL's stream, so copying a file URL onto its own file emptied it and
     * returned normally. Every peer operation refuses the same shape through
     * {@link #requireCanonicalPathsNotEquals(File, File)}, which also catches a hard-link alias; this reaches
     * that guard through the URL.
     *
     * <p>The check is deliberately <b>fail-open</b>, in two steps:
     * <ul>
     *   <li>Only the spellings the JDK's {@code file:} handler reads as a <i>local</i> file can name the
     *       destination at all - a non-local authority fails with {@code MalformedURLException} before a byte
     *       moves - so those are skipped rather than canonicalized, which for a UNC name can reach the
     *       network.</li>
     *   <li>A URL {@link #toFile(URL)} refuses (a percent-encoded path separator) or resolves to a path this
     *       platform cannot canonicalize (the legacy {@code "file:/C|/.."} drive form, which the JDK handler
     *       still opens) names nothing to compare. Those calls are left exactly as they were: rejecting them
     *       here would turn a copy that works today into an error.</li>
     * </ul>
     *
     * @param source the URL being copied from.
     * @param destination the file being copied to.
     * @throws IllegalArgumentException if {@code source} resolves to {@code destination}.
     */
    private static void requireSourceUrlIsNotTheDestination(final URL source, final File destination) throws IllegalArgumentException {
        if (!"file".equals(source.getProtocol())) {
            return;
        }

        final String host = source.getHost();

        if (!(Strings.isEmpty(host) || "localhost".equalsIgnoreCase(host) || "~".equals(host))) {
            return;
        }

        final File sourceFile;

        try {
            sourceFile = toFile(source);
        } catch (final IllegalArgumentException e) {
            logger.debug("Not comparing '{}' with the copy destination: it does not name a local file ({})", source, e.getMessage());

            return;
        }

        try {
            requireCanonicalPathsNotEquals(sourceFile, destination);
        } catch (final IOException e) {
            logger.debug("Not comparing '{}' with the copy destination: its path cannot be resolved ({})", source, e.getMessage());
        }
    }

    /**
     * Copies a file, or creates an empty directory if the source is a directory.
     * Directory <em>contents</em> are not copied; use {@link #copyToDirectory(File, File)} or
     * {@link #copyDirectory(File, File)} for a tree copy. Unlike {@link #copyFile(File, File)}, this
     * does not create missing parents, does not overwrite unless {@code REPLACE_EXISTING} is passed,
     * and does not preserve timestamps unless {@code COPY_ATTRIBUTES} is passed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume input.txt exists and input-copy.txt does not.
     * Path source = Path.of("input.txt");
     * Path target = Path.of("input-copy.txt");
     * Path result = IOUtil.copy(source, target);                          // returns target; file contents copied
     * // overwrite an existing target:
     * IOUtil.copy(source, target, StandardCopyOption.REPLACE_EXISTING);   // returns target
     * }</pre>
     *
     * @param source  the source path of the file or directory to be copied.
     * @param target  the target path where the file or directory will be copied to.
     * @param options optional arguments that specify how the copy should be done. Must not be {@code null};
     *                pass no arguments, or an empty array, for "no options".
     * @return the path to the target file or directory.
     * @throws IllegalArgumentException if {@code source}, {@code target} or {@code options} is {@code null}.
     * @throws FileNotFoundException if {@code source} does not exist, or a path required by the copy is missing.
     * @throws IOException if reading {@code source} or writing {@code target} fails
     * @see Files#copy(Path, Path, CopyOption...)
     */
    @SafeVarargs
    public static Path copy(final Path source, final Path target, final CopyOption... options) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(target, cs.target);
        N.checkArgNotNull(options, cs.options);

        try {
            return Files.copy(source, target, options);
        } catch (final NoSuchFileException e) {
            throw asFileNotFoundException(e);
        }
    }

    /**
     * Copies the content of the given InputStream to the specified target Path.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path target = Paths.get("output.dat");
     * try (InputStream in = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8))) {
     *     IOUtil.copy(in, target);                                       // copies to file (creates new)
     * }
     * try (InputStream in = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8))) {
     *     IOUtil.copy(in, target, StandardCopyOption.REPLACE_EXISTING);  // overwrites existing file
     * }
     * }</pre>
     *
     * @param in      the InputStream to be copied.
     * @param target  the target Path where the InputStream content will be copied to.
     * @param options optional arguments that specify how the copy should be done. Must not be {@code null};
     *                pass no arguments, or an empty array, for "no options".
     * @return the number of bytes read or skipped and written to the target Path.
     * @throws IllegalArgumentException if {@code in}, {@code target} or {@code options} is {@code null}.
     * @throws FileNotFoundException if a path required by the copy is missing.
     * @throws IOException if reading {@code in} or writing {@code target} fails.
     * @see Files#copy(InputStream, Path, CopyOption...)
     */
    @SafeVarargs
    public static long copy(final InputStream in, final Path target, final CopyOption... options) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(in, cs.input);
        N.checkArgNotNull(target, cs.target);
        N.checkArgNotNull(options, cs.options);

        try {
            return Files.copy(in, target, options);
        } catch (final NoSuchFileException e) {
            throw asFileNotFoundException(e);
        }
    }

    /**
     * Copies the content of the file at the given source Path to the specified OutputStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path source = Paths.get("input.dat");
     * try (OutputStream os = new FileOutputStream("output.dat")) {
     *     IOUtil.copy(source, os);  // copies file content to output stream
     * }
     * // Edge: empty file
     * Path emptyFile = Paths.get("empty.dat");
     * try (OutputStream os = new FileOutputStream("output.dat")) {
     *     IOUtil.copy(emptyFile, os);  // copies empty file, returns 0
     * }
     * }</pre>
     *
     * @param source the source Path of the file to be copied.
     * @param output the OutputStream where the file content will be copied to.
     * @return the number of bytes read or skipped and written to the OutputStream.
     * @throws IllegalArgumentException if {@code source} or {@code output} is {@code null}.
     * @throws FileNotFoundException if {@code source} does not exist.
     * @throws IOException if reading {@code source} or writing {@code output} fails.
     * @see Files#copy(Path, OutputStream)
     */
    public static long copy(final Path source, final OutputStream output) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(output, cs.output);

        try {
            return Files.copy(source, output);
        } catch (final NoSuchFileException e) {
            throw asFileNotFoundException(e);
        }
    }

    /**
     * <p>Moves a file from the source location to the destination directory, creating the destination directory if it doesn't exist.</p>
     *
     * <p>An existing destination entry is not replaced; see {@link #moveToDirectory(File, File)}.</p>
     *
     * @param srcFile the source file or directory to be moved, must not be {@code null}.
     * @param destDir the destination directory where the file or directory will be moved to.
     * @throws IllegalArgumentException if {@code srcFile} is {@code null} or is a filesystem root, or if {@code destDir}
     *         exists but is not a directory.
     * @throws FileNotFoundException if {@code srcFile} does not exist (a dangling link is a source: the link
     *         itself is what moves).
     * @throws IOException if relocating {@code srcFile} to {@code destDir} fails, such as if the destination cannot be created or written to.
     * @throws FileAlreadyExistsException if {@code destDir} already contains an entry with the
     *         source's name.
     * @deprecated the second argument is a destination <i>directory</i>, not a destination file (unlike
     *             {@link #copyFile(File, File)}), which the name {@code move} does not convey.
     *             Use {@link #moveToDirectory(File, File)} instead, or {@link #move(Path, Path, CopyOption...)}
     *             for a file-to-file move/rename.
     */
    @Deprecated
    public static void move(final File srcFile, final File destDir) throws IllegalArgumentException, IOException {
        moveToDirectory(srcFile, destDir);
    }

    /**
     * Moves a file from the source file to the target directory.
     * If the destination directory does not exist, it will be created.
     *
     * @param srcFile the source file to be moved.
     * @param destDir the target directory where the file will be moved to.
     * @param options optional arguments that specify how the move should be done.
     * @throws IllegalArgumentException if {@code srcFile}, {@code destDir} or {@code options} is {@code null},
     *         if {@code srcFile} is a filesystem root, if {@code destDir} exists but is not a directory, or if
     *         {@code srcFile} is a directory and {@code destDir} is that directory or is inside it.
     * @throws FileNotFoundException if {@code srcFile} does not exist (a dangling link is a source: the link
     *         itself is what moves).
     * @throws FileAlreadyExistsException if {@code destDir} already contains an entry with the
     *         source's name and {@link StandardCopyOption#REPLACE_EXISTING} was not passed.
     * @throws DirectoryNotEmptyException if replacement was requested and the existing
     *         destination entry is a non-empty directory.
     * @throws IOException if relocating {@code srcFile} to {@code destDir} fails, including failure to create the destination directory.
     * @deprecated the second argument is a destination <i>directory</i>, not a destination file, which the
     *             name {@code move} does not convey. Use {@link #moveToDirectory(File, File, CopyOption...)} instead.
     */
    @Deprecated
    @SafeVarargs
    public static void move(final File srcFile, final File destDir, final CopyOption... options) throws IllegalArgumentException, IOException {
        moveToDirectory(srcFile, destDir, options);
    }

    /**
     * Moves a file or directory into the destination directory, keeping its own name, creating the
     * destination directory if it doesn't exist. The name is resolved as {@code copyToDirectory} resolves it:
     * {@code x/.} moves as {@code x}, and a link (dangling or not) moves as the link, under the link's own name.
     *
     * <p>The second argument is a destination <i>directory</i> (mirroring {@link #copyToDirectory(File, File)}),
     * not a destination file; the source keeps its own name inside it. Use
     * {@link #move(Path, Path, CopyOption...)} for a file-to-file move/rename.</p>
     *
     * <p><b>An existing destination is never overwritten</b>, matching {@link #copyToDirectory(File, File)}:
     * if {@code destDir} already holds a <i>different</i> entry with the source's name, a
     * {@link java.nio.file.FileAlreadyExistsException} is thrown and nothing is moved. Pass
     * {@link StandardCopyOption#REPLACE_EXISTING} to {@link #moveToDirectory(File, File, CopyOption...)} to opt
     * into replacement. (Earlier versions replaced silently, which made the destructive half of the
     * copy/move pair the quiet one.)</p>
     *
     * <p>Moving something into the directory it is already in is a no-op that returns normally: the source and
     * the target are the same file, so there is nothing to move and nothing is removed. That is the one case in
     * which an entry with the source's name is already present and no exception follows - it is the same entry.</p>
     *
     * <p>When {@code srcFile} is a directory, {@code destDir} must not be that directory itself nor lie inside
     * it; that is rejected before the destination is created.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File sourceFile = new File("/path/to/source/file.txt");
     * File targetDir = new File("/path/to/destination");
     * IOUtil.moveToDirectory(sourceFile, targetDir);
     * // The file is now at /path/to/destination/file.txt and removed from the original location
     *
     * // to replace an entry that is already there:
     * IOUtil.moveToDirectory(sourceFile, targetDir, StandardCopyOption.REPLACE_EXISTING);
     * }</pre>
     *
     * @param srcFile the source file or directory to be moved, must not be {@code null}.
     * @param destDir the destination directory where the file or directory will be moved to.
     * @throws IllegalArgumentException if {@code srcFile} is {@code null} or is a filesystem root, if {@code destDir}
     *         exists but is not a directory, or if {@code srcFile} is a directory and {@code destDir} is that
     *         directory or is inside it.
     * @throws FileNotFoundException if {@code srcFile} does not exist (a dangling link is a source: the link
     *         itself is what moves).
     * @throws IOException if relocating {@code srcFile} to {@code destDir} fails, such as if the destination cannot be created or written to.
     * @throws FileAlreadyExistsException if {@code destDir} already contains a different entry
     *         with the source's name. (A non-empty directory of that name reports this too: without
     *         {@link StandardCopyOption#REPLACE_EXISTING} the move never gets as far as trying to merge, so
     *         {@link java.nio.file.DirectoryNotEmptyException} is reachable only through
     *         {@link #moveToDirectory(File, File, CopyOption...)}.)
     * @see #moveToDirectory(File, File, CopyOption...)
     * @see #copyToDirectory(File, File)
     */
    public static void moveToDirectory(final File srcFile, final File destDir) throws IllegalArgumentException, IOException {
        // Deliberately NO StandardCopyOption.REPLACE_EXISTING: copyToDirectory(..) refuses to overwrite an
        // existing destination (it even falls back to a "Copy of .." name), so a move that silently replaced
        // it made the operation that ALSO deletes the source the quieter of the two. Callers that want
        // replacement ask for it through the CopyOption overload.
        moveToDirectory(srcFile, destDir, new CopyOption[0]);
    }

    /**
     * Moves a file or directory into the destination directory, keeping its own name, creating the
     * destination directory if it doesn't exist. The name is resolved as {@code copyToDirectory} resolves it:
     * {@code x/.} moves as {@code x}, and a link (dangling or not) moves as the link, under the link's own name.
     *
     * <p>The second argument is a destination <i>directory</i>, not a destination file.</p>
     *
     * <p><b>{@link StandardCopyOption#REPLACE_EXISTING} replaces a <i>file</i>, not a populated directory.</b>
     * Moving a directory onto an existing non-empty directory of the same name fails with
     * {@link java.nio.file.DirectoryNotEmptyException} whether or not the option is given, because
     * {@link Files#move(Path, Path, CopyOption...)} will not merge trees. A directory move between two
     * filesystems fails for the same reason: this method does not fall back to copy-then-delete. Use
     * {@link #copyToDirectory(File, File)} followed by {@link #deleteRecursivelyIfExists(File)} when either
     * case is possible.</p>
     *
     * <p>When {@code srcFile} is a directory, {@code destDir} must not be that directory itself nor lie inside
     * it - a directory cannot be moved into its own subtree. That is rejected before the destination is
     * created, rather than surfacing as a platform-specific {@code FileSystemException} once it has been.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume data.txt exists and archive is the destination directory.
     * File srcFile = new File("data.txt");
     * File destDir = new File("archive");
     * IOUtil.moveToDirectory(srcFile, destDir, StandardCopyOption.REPLACE_EXISTING);
     * // srcFile no longer exists; destDir/data.txt now holds the content
     * }</pre>
     *
     * @param srcFile the source file to be moved, must not be {@code null}.
     * @param destDir the target directory where the file will be moved to.
     * @param options optional arguments that specify how the move should be done. Without
     *                {@link StandardCopyOption#REPLACE_EXISTING} an existing destination entry is not replaced.
     *                Must not be {@code null}; pass no arguments, or an empty array, for "no options".
     * @throws IllegalArgumentException if {@code srcFile} is {@code null} or is a filesystem root, if {@code options}
     *         is {@code null}, if {@code destDir} exists but is not a directory, or if {@code srcFile} is a
     *         directory and {@code destDir} is that directory or is inside it.
     * @throws FileNotFoundException if {@code srcFile} does not exist (a dangling link is a source: the link
     *         itself is what moves).
     * @throws IOException if relocating {@code srcFile} to {@code destDir} fails, including failure to create the destination directory.
     * @throws FileAlreadyExistsException if {@code destDir} already contains an entry with the
     *         source's name and {@link StandardCopyOption#REPLACE_EXISTING} was not given.
     * @throws DirectoryNotEmptyException if the source is a directory and the destination entry is
     *         an existing non-empty directory.
     * @see #moveToDirectory(File, File)
     * @see #copyToDirectory(File, File, boolean)
     */
    @SafeVarargs
    public static void moveToDirectory(final File srcFile, final File destDir, final CopyOption... options) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(srcFile, cs.srcFile);
        N.checkArgNotNull(options, cs.options);

        // A missing source is reported as the class contract says (FileNotFoundException, like copyToDirectory);
        // a dangling symbolic link or junction is a source too - the link itself is what moves - although
        // File.exists() follows it and would call it absent.
        if (!existsOrIsDanglingLink(srcFile)) {
            throw new FileNotFoundException("'" + describe(srcFile) + "' does not exist");
        }

        // "x/." is x: POSIX rename(2) refuses a path that ends in "." or "..", and the platform's own
        // resolution is what the name below is derived from anyway.
        final File source = withoutTrailingDotSegment(srcFile);
        final boolean isLink = isSymbolicLinkOrJunction(source);

        // Validate the destination before creating it, so a rejected call leaves no new directory behind.
        checkDestDirectory(destDir);

        if (!isLink && source.isDirectory()) {
            // A directory cannot be moved into itself or into one of its own descendants. Left to Files.move
            // that surfaced as a platform-specific failure ("The parameter is incorrect" on Windows, an
            // AccessDeniedException for the nested case) AFTER the destination had already been created
            // inside the source. Rejected here instead, matching copyToDirectory(..)/copyDirectory(..).
            requireDestDirectoryOutsideSourceDirectory(resolvedDirectoryLocation(source).toString(), resolvedDirectoryLocation(destDir).toString(), "move");
        }

        // The entry keeps the source's own name, resolved as copyToDirectory(..) resolves it: "x/." moves as "x"
        // and a link moves under the link's name. File.getName() answers "." or ".." for such a path, which would
        // have targeted destDir itself (replacing it under REPLACE_EXISTING) or destDir's parent.
        final String name = sourceName(source);

        if (name.isEmpty()) {
            throw new IllegalArgumentException("'" + describe(srcFile) + "' has no name to be moved under: a filesystem root cannot be moved into a directory");
        }

        createDestDirectory(destDir);

        move(source.toPath(), destDir.toPath().resolve(name), options);
    }

    /**
     * Moves a file from the source path to the target path.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path source = Paths.get("old_name.txt");
     * Path target = Paths.get("new_name.txt");
     * IOUtil.move(source, target);                                        // moves/renames file
     * IOUtil.move(source, target, StandardCopyOption.REPLACE_EXISTING);   // overwrites if target exists
     * }</pre>
     *
     * @param source  the source Path of the file to be moved.
     * @param target  the target Path where the file will be moved to.
     * @param options optional arguments that specify how the move should be done. Must not be {@code null};
     *                pass no arguments, or an empty array, for "no options".
     * @return the target path.
     * @throws IllegalArgumentException if {@code source}, {@code target} or {@code options} is {@code null}.
     * @throws FileNotFoundException if {@code source} does not exist, or a path required by the move is missing.
     * @throws IOException if relocating {@code source} to {@code target} fails
     * @see Files#move(Path, Path, CopyOption...)
     */
    @SafeVarargs
    public static Path move(final Path source, final Path target, final CopyOption... options) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(source, cs.source);
        N.checkArgNotNull(target, cs.target);
        N.checkArgNotNull(options, cs.options);

        try {
            return Files.move(source, target, options);
        } catch (final NoSuchFileException e) {
            throw asFileNotFoundException(e);
        }
    }

    /**
     * {@link Files#copy}/{@link Files#move} report a missing path as {@link NoSuchFileException}.
     * File twins and the class contract use {@link FileNotFoundException} for a missing source, so
     * callers catching the latter would miss the Path overloads.
     */
    private static FileNotFoundException asFileNotFoundException(final NoSuchFileException e) {
        final FileNotFoundException absent = new FileNotFoundException(e.getMessage());
        absent.initCause(e);
        return absent;
    }

    /**
     * Renames the specified source file to the new file name provided. The renamed file stays in the
     * source file's own parent directory; {@code newFileName} is a file name, not a path.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File oldFile = new File("dir/old_name.txt");
     * boolean success = IOUtil.renameTo(oldFile, "new_name.txt");   // the file becomes dir/new_name.txt
     * }</pre>
     *
     * @param srcFile     the source file to be renamed. Can be {@code null}, in which case {@code false} is returned.
     * @param newFileName the new name for the file; must be a single path element (no separators or {@code ..}).
     *                    It is validated even when {@code srcFile} is {@code null}, so a bad name is always
     *                    reported rather than masked by a {@code false} result.
     * @return {@code true} if the renaming succeeded, {@code false} otherwise (including when {@code srcFile} is
     *         {@code null}).
     * @throws IllegalArgumentException if {@code newFileName} is {@code null}, empty, {@code "."}, {@code ".."},
     *         or contains a path separator.
     * @see File#renameTo(File)
     * @see #move(Path, Path, CopyOption...)
     */
    public static boolean renameTo(final File srcFile, final String newFileName) throws IllegalArgumentException {
        // Validate the name FIRST, so the documented IllegalArgumentException does not depend on whether the
        // caller also happened to pass a null srcFile.
        N.checkArgNotEmpty(newFileName, cs.newFileName);

        if (newFileName.indexOf('/') >= 0 || newFileName.indexOf('\\') >= 0 || ".".equals(newFileName) || "..".equals(newFileName)) {
            throw new IllegalArgumentException("newFileName must be a single file name, not a path: " + newFileName);
        }

        if (srcFile == null) {
            return false;
        }

        final File parent = srcFile.getParentFile();
        final File newFile = parent == null ? new File(newFileName) : new File(parent, newFileName);
        return srcFile.renameTo(newFile);
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
     * For recursive deletion, use {@link #deleteRecursivelyIfExists(File)} instead.
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
     *         is {@code null}, does not exist (a dangling symbolic link counts as present and is unlinked, as in
     *         {@link #deleteIfExists(File)}), could not be deleted, or if an exception occurred.
     * @see File#delete()
     * @see Files#deleteIfExists(Path)
     * @see #deleteIfExists(File)
     * @see #deleteRecursivelyIfExists(File)
     */
    public static boolean deleteQuietly(final File file) {
        try {
            return deleteIfExists(file);
        } catch (final Exception e) {
            logger.error(e, "Failed to delete file: {}", file);
            return false;
        }
    }

    /**
     * Deletes the specified file if it exists by calling {@link File#delete()}.
     * <p>
     * This method attempts to delete the specified file or directory. If the file is {@code null}
     * or does not exist, the method returns {@code false} without attempting deletion.
     * For directories, this method only deletes empty directories - it will not delete
     * directories that contain files or subdirectories.
     * <p>
     * This is a simple deletion method that only removes the file itself. For recursive
     * deletion of directories and their contents, use {@link #deleteRecursivelyIfExists(File)} instead.
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
     * <p>A symbolic link is unlinked, never followed - including a <i>dangling</i> one, whose target is already
     * gone. {@link File#exists()} answers {@code false} for such a link, so it is not "absent" here: the link
     * itself is what gets removed.
     *
     * @param file the file or directory to delete. Can be {@code null}.
     * @return {@code true} if the file was deleted successfully; {@code false} if the file
     *         is {@code null}, does not exist (and is not a dangling symbolic link), or could not be deleted.
     * @see File#delete()
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     * @see #deleteRecursivelyIfExists(File)
     * @see #deleteQuietly(File)
     */
    public static boolean deleteIfExists(final File file) {
        if ((file == null) || !existsOrIsDanglingLink(file)) {
            return false;
        }

        return withoutTrailingDotSegment(file).delete(); //NOSONAR
    }

    /**
     * Whether {@code file} is there to be deleted: it exists, or it is a symbolic link whose target does not.
     *
     * <p>{@link File#exists()} follows a link, so a dangling one used to read as "nothing here" and the delete
     * family returned {@code false} and left the link behind - while the same link one level down, met during a
     * recursive walk, was unlinked. The link test is asked only once {@code exists()} has said no, so an ordinary
     * file still costs one stat, and a name the platform cannot even parse as a path stays "absent" rather than
     * turning into an {@link InvalidPathException} from a method that never threw one.
     *
     * @param file the file to test; must not be {@code null}.
     * @return {@code true} if {@code file} exists or is a dangling symbolic link or Windows junction.
     */
    private static boolean existsOrIsDanglingLink(final File file) {
        if (file.exists()) {
            return true;
        }

        try {
            // NOFOLLOW_LINKS sees the link entry itself, whatever kind of link it is: a dangling symbolic link on
            // every platform and a dangling directory junction on Windows (Files.isSymbolicLink is false for one).
            return Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS);
        } catch (final InvalidPathException e) {
            return false;
        }
    }

    /**
     * Whether {@code file} is a directory entry that redirects elsewhere and must therefore never be descended
     * into: a symbolic link on every platform, or a directory junction (mount point) on Windows.
     *
     * <p>{@link Files#isSymbolicLink(Path)} is {@code true} only for the {@code SYMLINK} reparse tag; a junction
     * (created with {@code mklink /J}, which needs no privilege) carries the {@code MOUNT_POINT} tag instead, so
     * it read as an ordinary directory here while {@link File#isDirectory()} and {@link File#listFiles()} followed
     * it - and the delete family emptied the junction's <i>target</i>. Under {@code NOFOLLOW_LINKS} a junction is
     * {@code isDirectory() && isOther()}; because other directory reparse points (cloud-storage placeholders,
     * projected file systems) look the same, a redirect is confirmed by the entry's real path differing from its
     * parent's real path plus its own name, and one whose target cannot be resolved at all (dangling) counts as a
     * link. On Unix {@code isOther()} never coincides with {@code isDirectory()}, so this is exactly
     * {@code isSymbolicLink()} there.
     *
     * <p><b>Internal API.</b> It is {@code public} only so that the directory-walking code in
     * {@code com.landawn.abacus.util.stream} can share this predicate rather than duplicate the
     * reparse-point handling above; it is not part of the supported surface and may change at any time.
     *
     * @param file the entry to classify; must not be {@code null}.
     * @return {@code true} if the entry is a symbolic link or a Windows directory junction.
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     */
    @Beta
    public static boolean isSymbolicLinkOrJunction(final File file) throws IllegalArgumentException {
        // Now that this is public, it must reject null the way every other public File-taking method
        // here does (IllegalArgumentException, not NPE) - an invariant IOUtilNullPathWriteTest enforces.
        N.checkArgNotNull(file, cs.file);

        final Path path;

        try {
            path = file.toPath();
        } catch (final InvalidPathException e) {
            return false;
        }

        final BasicFileAttributes attrs;

        try {
            attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (final IOException e) {
            return false;
        }

        return isSymbolicLinkOrJunction(path, attrs);
    }

    /**
     * The {@link #isSymbolicLinkOrJunction(File)} test for an entry whose {@code NOFOLLOW_LINKS} attributes are
     * already in hand (a {@link Files#walkFileTree} callback).
     *
     * @param path the entry; must not be {@code null}.
     * @param noFollowAttrs its attributes read without following links.
     * @return {@code true} if the entry is a symbolic link or a Windows directory junction.
     */
    private static boolean isSymbolicLinkOrJunction(final Path path, final BasicFileAttributes noFollowAttrs) {
        if (noFollowAttrs.isSymbolicLink()) {
            return true;
        }

        if (!IS_OS_WINDOWS || !noFollowAttrs.isDirectory() || !noFollowAttrs.isOther()) {
            return false;
        }

        try {
            final Path parent = path.toAbsolutePath().getParent();
            final Path fileName = path.getFileName();

            if (parent == null || fileName == null) {
                return false;
            }

            return !path.toRealPath().equals(parent.toRealPath().resolve(fileName));
        } catch (final IOException e) {
            return true; // the redirect cannot be resolved: a dangling junction
        }
    }

    /**
     * What an {@code append} of nothing does to its target: opens it in append mode and closes it again.
     *
     * <p>That is exactly what the non-empty twin does before writing, so the two agree on every destination: a
     * missing file is created (with its parent directories) and an existing one is never truncated, while a
     * directory is a bad argument ({@link IllegalArgumentException}) and a file that cannot be opened for writing
     * is a {@link FileNotFoundException}. The short-circuit used to call {@code createNewFileIfNotExists} instead,
     * which answers "already there" for a directory or a read-only file and so let {@code append("", dir)} succeed
     * silently where {@code append("x", dir)} throws.
     *
     * @param targetFile the append destination; must not be {@code null}.
     * @throws IOException if the target cannot be created or opened for appending.
     */
    private static void openAppendTargetOnly(final File targetFile) throws IOException {
        openFileOutputStream(targetFile, true).close();
    }

    /**
     * {@code file} with a final {@code "."} or {@code ".."} element folded away by the platform - the path
     * {@code toRealPath(NOFOLLOW_LINKS)} produces, so a final link is kept as the link. POSIX {@code rename(2)} and
     * {@code rmdir(2)} refuse a path that ends in such an element, so {@code moveToDirectory(new File("x/."), dir)}
     * failed there after the destination had been created, and {@code deleteRecursivelyIfExists(new File("x/."))}
     * emptied {@code x} and then answered {@code false}; Windows folds the element silently, which is why neither
     * showed up on the project's own platform. Any other path is returned as it is, and a path the platform cannot
     * resolve (it is gone, or unreadable) is returned as it is too, so the caller reports that in its own terms.
     */
    private static File withoutTrailingDotSegment(final File file) {
        final String name = file.getName();

        if (!".".equals(name) && !"..".equals(name)) {
            return file;
        }

        try {
            return file.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toFile();
        } catch (final IOException | InvalidPathException e) {
            return file;
        }
    }

    /**
     * Deletes the specified file and all its subfiles/directories recursively if it's a directory.
     * <p>
     * This method performs a recursive deletion operation. If the specified file is a directory,
     * it will delete all files and subdirectories within it before deleting the directory itself.
     * If the file is a regular file, it will simply delete the file. If the file does not exist
     * or is {@code null}, the method returns {@code false} without performing any operations.
     * <p>
     * This operation is irreversible and will permanently remove all specified files and directories.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("./temp-folder");
     * boolean deleted = IOUtil.deleteRecursivelyIfExists(directory);
     * if (deleted) {
     *     System.out.println("File/directory and all contents deleted successfully.");
     * } else {
     *     System.out.println("File/directory does not exist or could not be deleted.");
     * }
     * }</pre>
     *
     * <p>Deletion is best-effort and <b>not transactional</b>: every entry is attempted, one that cannot be
     * removed does not stop the rest of the tree, and {@code false} is returned at the end if any of them
     * failed - so a {@code false} result normally means the tree was <i>partially</i> removed rather than left
     * untouched. Nothing is restored. A directory whose contents did not all go away is not itself attempted,
     * since removing it could not succeed. This matches {@link #deleteFilesFromDirectory(File)}, which empties
     * a directory the same way but keeps the directory itself.
     *
     * <p>Symbolic links are unlinked, never followed, so deleting a tree that contains a link cannot remove
     * files outside it and cannot loop on a cyclic link. That includes a <i>dangling</i> link handed in directly:
     * {@link File#exists()} answers {@code false} for one, but it is not "absent" here - the link itself is removed,
     * exactly as it would be when met inside a tree.
     *
     * @param file the file or directory to delete recursively.
     * @return {@code true} if the file/directory and all its contents were deleted successfully;
     *         {@code false} if the file is {@code null}, does not exist (and is not a dangling symbolic link), or
     *         any part of it could not be deleted (in which case part of the tree may already be gone).
     * @see File#delete()
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     * @see #deleteIfExists(File)
     * @see #deleteFilesFromDirectory(File)
     */
    public static boolean deleteRecursivelyIfExists(final File file) {
        if ((file == null) || !existsOrIsDanglingLink(file)) {
            return false;
        }

        // "x/." is x: POSIX refuses to rmdir a path that ends in "." or "..", so the tree was emptied and then
        // reported as not deleted.
        final File target = withoutTrailingDotSegment(file);

        // Symlinked directories must NOT be traversed; deleting their contents would wipe
        // files outside the tree. Just unlink the symlink itself.
        //
        // Short-circuited deliberately: a directory that still holds something cannot be removed, so the
        // delete would fail and tell the caller nothing it does not already know.
        if (target.isDirectory() && !isSymbolicLinkOrJunction(target)) {
            return deleteDirectoryContents(target) && target.delete(); //NOSONAR
        }

        return target.delete(); //NOSONAR
    }

    /**
     * Removes everything below {@code dir}, best effort, and reports whether all of it went.
     *
     * <p>{@code dir} is a real directory the caller has already classified, so nothing is re-validated per
     * level: recursing through the public {@link #deleteRecursivelyIfExists(File)} instead cost every
     * subdirectory a second {@code exists()}/{@code isDirectory()}/{@code isSymbolicLink()} round. Each entry is
     * classified with one stat pair - a link of any kind is unlinked and never followed, anything else that is
     * not a directory (a file, a FIFO, a device node) is simply deleted, and only a real directory is descended
     * into.
     *
     * <p>Accumulated rather than returned on the first failure: one undeletable entry does not stop the rest of
     * the tree from being attempted. Stopping early left MORE behind than the caller had any way to discover - a
     * locked file among six siblings left four of them in place - and disagreed with the sibling
     * {@link #deleteFilesFromDirectory(File)}, which walks the same shape best-effort.
     *
     * @param dir the directory to empty; must exist, be a directory, and not be a symbolic link.
     * @return {@code true} if every entry below {@code dir} was deleted.
     */
    private static boolean deleteDirectoryContents(final File dir) {
        final File[] files = dir.listFiles();

        if (files == null) {
            // null means an I/O error, not an empty directory (this is known to BE a directory). The
            // contents are therefore unknown, so report failure rather than delete something unexamined.
            return false;
        }

        boolean allDeleted = true;

        for (final File subFile : files) {
            if (subFile == null) {
                continue;
            }

            if (isSymbolicLinkOrJunction(subFile) || !subFile.isDirectory()) {
                if (!subFile.delete()) { //NOSONAR
                    allDeleted = false;
                }
            } else if (!(deleteDirectoryContents(subFile) && subFile.delete())) { //NOSONAR
                allDeleted = false;
            }
        }

        return allDeleted;
    }

    /**
     * Deletes all subfiles and subdirectories from the specified directory.
     * <p>
     * This method removes all files and directories contained within the specified directory,
     * but leaves the directory itself intact. If the directory does not exist or is actually
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
     * <p>Deletion is best-effort and <b>not transactional</b>: every entry is attempted, and {@code false} is
     * returned at the end if any of them failed, so a {@code false} result normally means the directory was
     * <i>partially</i> emptied. Nothing is restored. {@link #deleteRecursivelyIfExists(File)} walks the same
     * shape with the same best-effort semantics; the difference is that it also removes {@code dir} itself.
     *
     * @param dir the directory from which to delete all files and subdirectories. Can be {@code null}.
     * @return {@code true} if all files and directories were deleted successfully;
     *         {@code false} if {@code dir} is {@code null}, does not exist, is actually a file or a symbolic link,
     *         or some files could not be deleted or if the operation failed.
     * @see File#delete()
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     * @see #deleteFilesFromDirectory(File, Throwables.BiPredicate)
     * @see #deleteRecursivelyIfExists(File)
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
     * <p><b>The filter is applied to every descendant, not only to the immediate children</b> - the same rule
     * {@link #copyToDirectory(File, File, boolean, Throwables.BiPredicate)} follows. Every subdirectory is
     * descended into whether or not the filter accepted it, and a subdirectory is itself removed only when the
     * filter accepted it <i>and</i> nothing inside it survived the filter. An accepted directory is therefore
     * never deleted wholesale, so a filter written to protect files - {@code f -> f.getName().endsWith(".tmp")} -
     * cannot be defeated by a directory whose own <i>name</i> happens to match it.
     *
     * <p>Symbolic links are unlinked, never followed: a link that the filter accepts is removed, and its target
     * directory is neither descended into nor deleted.
     *
     * <p>Deletion is best-effort and <b>not transactional</b>: every entry is attempted, and {@code false} is
     * returned at the end if any of them failed, so a {@code false} result normally means the tree was
     * <i>partially</i> deleted. Nothing is restored.
     *
     * <p><b>{@code false} is also the answer when the protection above kicks in.</b> A directory the filter
     * accepted but which still holds survivors cannot be removed, and that counts as an accepted entry that was
     * not deleted. So filtering a tree whose matching directories contain non-matching files reports
     * {@code false} even though it behaved exactly as intended - the result says "something you asked to delete
     * is still there", not "something went wrong".
     *
     * @param <E> the type of exception that the filter may throw.
     * @param dir the directory from which to delete files and subdirectories. Can be {@code null}.
     * @param filter the predicate to determine which files/directories should be deleted.
     *               Receives the parent directory and the file/directory being evaluated. It is consulted for
     *               every entry at every depth.
     * @return {@code true} if every entry the filter accepted was deleted successfully;
     *         {@code false} if {@code dir} is {@code null}, does not exist, is actually a file or a symbolic link,
     *         or if any accepted entry could not be deleted.
     * @throws IllegalArgumentException if {@code filter} is {@code null}.
     * @throws E if the filter throws an exception during evaluation.
     * @see File#delete()
     * @see Files#delete(Path)
     * @see Files#deleteIfExists(Path)
     * @see #deleteFilesFromDirectory(File)
     * @see #deleteRecursivelyIfExists(File)
     */
    public static <E extends Exception> boolean deleteFilesFromDirectory(final File dir, final Throwables.BiPredicate<? super File, ? super File, E> filter)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(filter, cs.filter);

        if ((dir == null) || !dir.exists() || dir.isFile() || isSymbolicLinkOrJunction(dir)) {
            return false;
        }

        final File[] files = dir.listFiles();

        if (files == null) {
            return false;
        }

        if (files.length == 0) {
            return true;
        }

        // Accumulated rather than returned on the first failure, as this used to do: one undeletable entry no
        // longer stops the rest of the tree from being processed. The result still reports whether every entry
        // the filter accepted actually went away.
        boolean allDeleted = true;

        for (final File subFile : files) {
            if (subFile == null) {
                continue;
            }

            // A symbolic link (or a Windows junction) is unlinked, never followed: deleting through it would
            // remove files outside this tree, and descending into it could loop on a cycle.
            final boolean isSymlink = isSymbolicLinkOrJunction(subFile);

            // Anything that is not a real directory - a regular file, a link of any kind, or a special file such
            // as a device node or FIFO - is simply unlinked when the filter accepts it.
            if (isSymlink || !subFile.isDirectory()) {
                if (filter.test(dir, subFile) && !subFile.delete()) { //NOSONAR
                    allDeleted = false;
                }

                continue;
            }

            // A directory is descended into whether or not the filter accepted it, and the filter is applied
            // to every descendant - matching doCopyDirectory. The directory itself is only removed when the
            // filter accepted it AND nothing inside it survived; an accepted directory is never deleted
            // wholesale, so a filter written to protect files cannot be defeated by a directory whose *name*
            // happens to match.
            final boolean accepted = filter.test(dir, subFile);

            if (!deleteFilesFromDirectory(subFile, filter)) {
                allDeleted = false;
            } else if (accepted && !subFile.delete()) { //NOSONAR
                allDeleted = false;
            }
        }

        return allDeleted;
    }

    /**
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     * @throws IOException if a required parent directory or the file cannot be created.
     */
    static boolean createNewFileIfNotExists(final File file) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);

        if (file.exists()) {
            return false;
        }

        final File parent = file.getParentFile();

        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Failed to create parent directory: " + describe(parent));
        }

        // File.createNewFile() is atomic: false also correctly reports a concurrent creator.
        return file.createNewFile();
    }

    /**
     * Creates a new empty file if one doesn't already exist at the specified path.
     * <p>
     * This method attempts to create a new file at the path specified by the input File
     * object, but only if a file or directory doesn't already exist at that location.
     * If the parent directory doesn't exist, the method will attempt to create it before
     * creating the file.
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
     * @param file the File object representing the file to create.
     * @return {@code true} if a new file was created successfully; {@code false} if an entry already exists at that
     *         path - a dangling symbolic link included, which {@code File.createNewFile()} counts as existing
     *         (nothing is created through it; {@link #touch(File)} does create the link's target).
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     * @throws UncheckedIOException if the filesystem cannot create {@code file}.
     * @see File#createNewFile()
     * @see #mkdirIfNotExists(File)
     * @see #mkdirsIfNotExists(File)
     */
    public static boolean createFileIfNotExists(final File file) throws IllegalArgumentException, UncheckedIOException {
        try {
            return createNewFileIfNotExists(file);
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to create file: " + describe(file), e);
        }
    }

    /**
     * Creates a directory if it does not already exist.
     * <p>
     * This method attempts to create the directory named by the given {@code File} object.
     * If a directory with this name already exists, the method returns {@code false}.
     * If the directory does not exist, it is created. This method does not create parent
     * directories. Use {@link #mkdirsIfNotExists(File)} to create parent directories as well.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File newDir = new File("./new-directory");
     * boolean result = IOUtil.mkdirIfNotExists(newDir);
     * if (result) {
     *     System.out.println("Directory was created successfully.");
     * } else {
     *     System.out.println("Directory already exists or creation failed.");
     * }
     * }</pre>
     *
     * <p><b>{@code false} is not a failure signal.</b> It is returned both when the directory already existed and
     * when creation failed, so it cannot be used as an error check. Test {@link #isDirectory(File)} afterwards to
     * find out whether the directory is actually there.
     *
     * @param dir the directory to create, must not be {@code null}.
     * @return {@code true} if the directory was created by this call; {@code false} if it already existed <i>or</i>
     *         if creation failed.
     * @throws IllegalArgumentException if {@code dir} is {@code null}.
     * @see File#mkdir()
     * @see #mkdirsIfNotExists(File)
     * @see #createFileIfNotExists(File)
     * @see #isDirectory(File)
     */
    public static boolean mkdirIfNotExists(final File dir) throws IllegalArgumentException {
        N.checkArgNotNull(dir, cs.dir);

        // isDirectory() already implies exists(); an existing *file* is not a directory, so mkdir() is attempted
        // and simply returns false for it.
        return !dir.isDirectory() && dir.mkdir();
    }

    /**
     * Creates new directories if they do not exist.
     * <p>
     * This method attempts to create the directory named by the given {@code File} object,
     * including any necessary but nonexistent parent directories. If a directory with this
     * name already exists, the method returns {@code false}. If the directory does not exist,
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
     * <p><b>{@code false} is not a failure signal.</b> It is returned both when the directory already existed and
     * when creation failed, so it cannot be used as an error check. Test {@link #isDirectory(File)} afterwards to
     * find out whether the directory is actually there.
     *
     * @param dir the directory to create, including any necessary parent directories. Must not be {@code null}.
     * @return {@code true} if the directories were created by this call; {@code false} if the directory already
     *         existed <i>or</i> if creation failed.
     * @throws IllegalArgumentException if {@code dir} is {@code null}.
     * @see File#mkdirs()
     * @see #mkdirIfNotExists(File)
     * @see #createFileIfNotExists(File)
     * @see #isDirectory(File)
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean mkdirsIfNotExists(final File dir) throws IllegalArgumentException {
        N.checkArgNotNull(dir, cs.dir);

        // isDirectory() already implies exists(); an existing *file* is not a directory, so mkdirs() is attempted
        // and simply returns false for it.
        return !dir.isDirectory() && dir.mkdirs();
    }

    /**
     * Checks if the provided Reader is an instance of {@code java.io.BufferedReader}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader br = new BufferedReader(new StringReader("x"));
     * boolean a = IOUtil.isBufferedReader(br);   // returns true
     * Reader sr = new StringReader("x");
     * boolean b = IOUtil.isBufferedReader(sr);   // returns false
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Writer bw = new BufferedWriter(new FileWriter("test.txt"))) {
     *     boolean result = IOUtil.isBufferedWriter(bw);  // returns true
     * }
     * try (Writer fw = new FileWriter("test.txt")) {
     *     boolean result = IOUtil.isBufferedWriter(fw);  // returns false
     * }
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("document.txt");
     * Date referenceDate = new Date(System.currentTimeMillis() - 86400000);  // 1 day ago
     * boolean isNewer = IOUtil.isFileNewer(file, referenceDate);
     * }</pre>
     *
     * @param file the {@code File} of which the modification date must be compared.
     * @param date the date reference.
     * @return {@code true} if the {@code File} exists and has been modified after the given {@code Date}.
     *         A file that does not exist is neither newer nor older than the reference.
     * @throws IllegalArgumentException if the file or date is {@code null}.
     * @see #isFileOlder(File, Date)
     */
    public static boolean isFileNewer(final File file, final Date date) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(date, cs.date);

        // A file that does not exist is never "newer" than any reference (its lastModified() is 0, which
        // would otherwise wrongly test as newer than a pre-epoch/negative reference time).
        return file.exists() && file.lastModified() > date.getTime();
    }

    /**
     * Checks if the specified {@code File} is newer than the reference {@code File}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("document.txt");
     * File referenceFile = new File("reference.txt");
     * boolean isNewer = IOUtil.isFileNewer(file, referenceFile);
     * }</pre>
     *
     * @param file      the {@code File} of which the modification date must be compared.
     * @param reference the {@code File} of which the modification date is used. It must exist: an absent
     *                  reference has no modification time to compare against.
     * @return {@code true} if the {@code File} exists and has been modified more recently than the reference {@code File}.
     *         A file that does not exist is neither newer nor older than the reference.
     * @throws IllegalArgumentException if the file or reference file is {@code null}, or if {@code reference}
     *         does not exist.
     * @see #isFileOlder(File, File)
     */
    public static boolean isFileNewer(final File file, final File reference) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        // A missing reference used to read as epoch 0, so EVERY existing file tested as newer than it.
        requireExistingReference(reference);

        // A file that does not exist is never "newer" than the reference (its lastModified() is 0, which
        // would otherwise wrongly test as newer than a pre-epoch/negative reference time).
        return file.exists() && file.lastModified() > reference.lastModified();
    }

    /**
     * Rejects a reference file that does not exist. {@link File#lastModified()} reports {@code 0} for an absent
     * file, so without this guard every existing file would silently test as "newer than" a reference that was
     * never created - a false answer rather than a reported one. Matches Apache Commons-IO, whose
     * {@code isFileNewer}/{@code isFileOlder} this pair mirrors.
     *
     * @param reference the reference file to validate.
     * @throws IllegalArgumentException if {@code reference} is {@code null} or does not exist.
     */
    private static void requireExistingReference(final File reference) throws IllegalArgumentException {
        N.checkArgNotNull(reference, cs.reference);

        if (!reference.exists()) {
            throw new IllegalArgumentException("The reference file '" + describe(reference) + "' does not exist");
        }
    }

    /**
     * Checks if the specified {@code File} is older than the specified {@code Date}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("document.txt");
     * Date referenceDate = new Date();
     * boolean isOlder = IOUtil.isFileOlder(file, referenceDate);
     * }</pre>
     *
     * @param file the {@code File} of which the modification date must be compared.
     * @param date the date reference.
     * @return {@code true} if the {@code File} exists and has been modified before the given {@code Date}.
     *         A file that does not exist is neither newer nor older than the reference: {@code false} is returned,
     *         matching {@link #isFileNewer(File, Date)}. Check {@link #isFile(File)} first if "absent" must be
     *         distinguished from "present but not older".
     * @throws IllegalArgumentException if the file or date is {@code null}.
     * @see #isFileNewer(File, Date)
     */
    public static boolean isFileOlder(final File file, final Date date) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgNotNull(date, cs.date);

        // A file that does not exist has no modification time to compare (File.lastModified() reports 0 for it,
        // which would otherwise test as older than every post-epoch reference). isFileNewer applies the same
        // guard, so the two stay exact complements for files that do exist.
        return file.exists() && file.lastModified() < date.getTime();
    }

    /**
     * Checks if the specified {@code File} is older than the reference {@code File}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("document.txt");
     * File referenceFile = new File("reference.txt");
     * boolean isOlder = IOUtil.isFileOlder(file, referenceFile);
     * }</pre>
     *
     * @param file      the {@code File} of which the modification date must be compared.
     * @param reference the {@code File} of which the modification date is used. It must exist: an absent
     *                  reference has no modification time to compare against.
     * @return {@code true} if the {@code File} exists and has been modified before the reference {@code File}.
     *         A file that does not exist is neither newer nor older than the reference: {@code false} is returned,
     *         matching {@link #isFileNewer(File, File)}.
     * @throws IllegalArgumentException if the file or reference file is {@code null}, or if {@code reference}
     *         does not exist.
     * @see #isFileNewer(File, File)
     */
    public static boolean isFileOlder(final File file, final File reference) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);
        // See isFileNewer(..): a missing reference used to read as epoch 0.
        requireExistingReference(reference);

        // See isFileOlder(File, Date): a non-existent file has no modification time to compare.
        return file.exists() && file.lastModified() < reference.lastModified();
    }

    /**
     * Checks if the specified {@code File} is a file or not.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume document.txt is an existing regular file.
     * File file = new File("document.txt");
     * boolean a = IOUtil.isFile(file);                         // returns true
     * boolean b = IOUtil.isFile(file.getParentFile());         // returns false (it is a directory)
     * boolean c = IOUtil.isFile(new File("does_not_exist"));   // returns false
     * boolean d = IOUtil.isFile(null);                         // returns false
     * }</pre>
     *
     * <p>{@link #isRegularFile(File, LinkOption...)} answers the same question through NIO and additionally
     * lets a symbolic link be left unfollowed; this method always follows one.
     *
     * @param file the {@code File} to check.
     * @return {@code true} if the {@code File} exists and is a file, {@code false} otherwise.
     * @see #isRegularFile(File, LinkOption...)
     * @see #isDirectory(File)
     * @see File#isFile()
     */
    public static boolean isFile(final File file) {
        return file != null && file.isFile();
    }

    /**
     * Checks if the specified {@code File} is a directory or not.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File path = new File("some_path");
     * boolean isDir = IOUtil.isDirectory(path);
     * }</pre>
     *
     * @param file the {@code File} to check.
     * @return {@code true} if the {@code File} exists and is a directory, {@code false} otherwise.
     * @see File#isDirectory()
     */
    public static boolean isDirectory(final File file) {
        return file != null && file.isDirectory();
    }

    /**
     * Tests whether the specified {@link File} is a directory or not. Implemented as a
     * null-safe delegate to {@link Files#isDirectory(Path, LinkOption...)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File path = new File("some_path");
     * boolean isDir = IOUtil.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
     * }</pre>
     *
     * @param   file the path to the file.
     * @param   options options indicating how symbolic links are handled.
     * @return  {@code true} if the file is a directory; {@code false} if
     *          the path is {@code null}, the file does not exist, is not a directory, or it cannot
     *          be determined if the file is a directory or not.
     * @throws SecurityException     In the case of the default provider, and a security manager is installed, the
     *                               {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read
     *                               access to the directory.
     * @see Files#isDirectory(Path, LinkOption...)
     */
    @SafeVarargs
    public static boolean isDirectory(final File file, final LinkOption... options) throws SecurityException {
        if (file == null) {
            return false;
        }

        try {
            return Files.isDirectory(file.toPath(), options);
        } catch (final InvalidPathException e) {
            return false;
        }
    }

    /**
     * Tests whether the specified {@link File} is a regular file or not. Implemented as a
     * null-safe delegate to {@link Files#isRegularFile(Path, LinkOption...)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File path = new File("document.txt");
     * boolean isRegular = IOUtil.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
     * }</pre>
     *
     * @param   file the path to the file.
     * @param   options options indicating how symbolic links are handled.
     * @return  {@code true} if the file is a regular file; {@code false} if
     *          the path is {@code null}, the file does not exist, is not a regular file, or it cannot
     *          be determined if the file is a regular file or not.
     * @throws SecurityException     In the case of the default provider, and a security manager is installed, the
     *                               {@link SecurityManager#checkRead(String) checkRead} method is invoked to check read
     *                               access to the file.
     * @see #isFile(File)
     * @see Files#isRegularFile(Path, LinkOption...)
     */
    @SafeVarargs
    public static boolean isRegularFile(final File file, final LinkOption... options) throws SecurityException {
        if (file == null) {
            return false;
        }

        try {
            return Files.isRegularFile(file.toPath(), options);
        } catch (final InvalidPathException e) {
            return false;
        }
    }

    /**
     * Checks if the specified file is a Symbolic Link rather than an actual file.
     *
     * <p>This is {@link Files#isSymbolicLink(Path)}'s answer, so a Windows directory junction ({@code mklink /J})
     * is {@code false} here; the listing, size and delete families of this class nevertheless treat a junction
     * as a linked directory - never descended into or deleted through - as the class documentation describes.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File path = new File("symlink");
     * boolean isSymlink = IOUtil.isSymbolicLink(path);
     * }</pre>
     *
     * @param file the file to be checked.
     * @return {@code true} if the file is a Symbolic Link, {@code false} otherwise.
     * @see Files#isSymbolicLink(Path)
     */
    public static boolean isSymbolicLink(final File file) {
        if (file == null) {
            return false;
        }

        try {
            return Files.isSymbolicLink(file.toPath());
        } catch (final InvalidPathException e) {
            return false;
        }
    }

    /**
     * Returns the size of the specified file or directory. If the provided
     * {@link File} is a regular file, then the file's length is returned.
     * If the argument is a directory, then the size of the directory is
     * calculated recursively. A subdirectory that cannot be listed contributes nothing; the argument itself
     * must be readable, and one that is not is reported rather than counted as empty - the same answer
     * {@link #sizeOfDirectory(File)} gives.
     * <p>
     * A total larger than {@link Long#MAX_VALUE} is reported as a negative number and stops the walk at the
     * entry that pushed the sum negative, so the value is a signal rather than a measurement. See
     * {@link #sizeOfAsBigInteger(File)} for the overflow-free counterpart.
     * </p>
     *
     * <p><b>Symbolic links:</b> a link handed to this method directly is followed and reports the size of its
     * target; a link found while walking a directory is skipped entirely. So a tree's reported size never
     * counts one file twice through a link, and never reaches outside the tree.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("document.pdf");
     * long size = IOUtil.sizeOf(file);
     * System.out.println("File size: " + size + " bytes");
     * }</pre>
     *
     * @param file the regular file or directory to return the size of (must not be {@code null}).
     * @return the length of the file, or recursive size of the directory (in bytes). A negative result means
     *         the real total exceeded {@link Long#MAX_VALUE}; it is a signal, not a measurement, and carries
     *         no usable magnitude, since the walk stops at the entry that pushed the sum negative.
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     * @throws UncheckedIOException if the file does not exist or is not readable.
     * @see #sizeOfAsBigInteger(File)
     */
    public static long sizeOf(final File file) throws IllegalArgumentException, UncheckedIOException {
        return sizeOf(file, false);
    }

    /**
     * Returns the size of the specified file or directory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume greeting.txt contains the five bytes "hello".
     * File file = new File("greeting.txt");
     * long size = IOUtil.sizeOf(file, false);   // returns 5
     * File missing = new File("missing_file.tmp");
     * long zero = IOUtil.sizeOf(missing, true);   // returns 0 (treated as empty)
     * // IOUtil.sizeOf(missing, false);           // throws UncheckedIOException (wrapping FileNotFoundException)
     * }</pre>
     *
     * @param file the file or directory whose size is to be calculated; {@code null} is treated as non-existing and yields 0 only when
     *        {@code considerNonExistingFileAsEmpty} is {@code true}
     * @param considerNonExistingFileAsEmpty if {@code true}, the size of non-existing file is considered as 0.
     * @return the total size in bytes. For directories, this is the recursive sum of all files within it,
     *         with symbolic links found during the walk skipped. A negative number means the real total is
     *         greater than {@link Long#MAX_VALUE}: it is a signal, not a measurement, since the walk stops at
     *         the entry that pushed the sum negative. See {@link #sizeOfAsBigInteger(File)} for the
     *         overflow-free counterpart.
     * @throws IllegalArgumentException if {@code file} is {@code null} and {@code considerNonExistingFileAsEmpty} is {@code false}.
     * @throws UncheckedIOException if the file does not exist and {@code considerNonExistingFileAsEmpty} is {@code false}.
     */
    public static long sizeOf(final File file, final boolean considerNonExistingFileAsEmpty) throws IllegalArgumentException, UncheckedIOException {
        if ((file == null || !file.exists()) && considerNonExistingFileAsEmpty) {
            return 0;
        }

        try {
            checkFileExists(file, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        if (file.isDirectory()) {
            return sizeOfDirectory0(file); // private method; expects directory
        }

        return file.length();
    }

    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     * <p>
     * A total larger than {@link Long#MAX_VALUE} is reported as a negative number and stops the walk at the
     * entry that pushed the sum negative, so the value is a signal rather than a measurement. See
     * {@link #sizeOfDirectoryAsBigInteger(File)} for the overflow-free counterpart.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("data");
     * long totalSize = IOUtil.sizeOfDirectory(directory);
     * System.out.println("Directory size: " + totalSize + " bytes");
     * }</pre>
     *
     * @param directory directory to inspect, must not be {@code null}.
     * @return size of directory in bytes, with symbolic links found during the walk skipped and an unreadable
     *         subdirectory contributing nothing; a negative number when the real total is greater than
     *         {@link Long#MAX_VALUE}, which is a signal rather than a measurement (the walk stops at the entry
     *         that pushed the sum negative).
     * @throws IllegalArgumentException if {@code directory} is {@code null}, or if the path exists but is not a directory.
     * @throws UncheckedIOException if the directory does not exist or cannot be read (wrapping a
     *         {@link FileNotFoundException}), exactly as {@link #sizeOf(File)} reports it.
     * @see #sizeOfDirectoryAsBigInteger(File)
     */
    public static long sizeOfDirectory(final File directory) throws IllegalArgumentException, UncheckedIOException {
        return sizeOfDirectory(directory, false);
    }

    /**
     * Counts the size of a directory recursively (sum of the length of all files).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("data");
     * long totalSize = IOUtil.sizeOfDirectory(directory, true);
     * System.out.println("Directory size: " + totalSize + " bytes");
     * }</pre>
     *
     * @param directory the directory whose size is to be calculated; {@code null} is treated as non-existing and yields 0 only when
     *        {@code considerNonExistingDirectoryAsEmpty} is {@code true}
     * @param considerNonExistingDirectoryAsEmpty if {@code true}, the size of non-existing directory is considered as 0.
     * @return the total size in bytes of all files within the directory and its subdirectories, with symbolic
     *         links found during the walk skipped. A negative number means the real total is greater than
     *         {@link Long#MAX_VALUE}: it is a signal, not a measurement, since the walk stops at the entry that
     *         pushed the sum negative. See {@link #sizeOfDirectoryAsBigInteger(File)} for the overflow-free
     *         counterpart.
     * @throws IllegalArgumentException if {@code directory} is {@code null} and {@code considerNonExistingDirectoryAsEmpty} is {@code false}, or if the path exists but is not a directory.
     * @throws UncheckedIOException if the directory does not exist and {@code considerNonExistingDirectoryAsEmpty} is {@code false},
     *         or if it exists but cannot be read.
     */
    public static long sizeOfDirectory(final File directory, final boolean considerNonExistingDirectoryAsEmpty)
            throws IllegalArgumentException, UncheckedIOException {
        if ((directory == null || !directory.exists()) && considerNonExistingDirectoryAsEmpty) {
            return 0;
        }

        try {
            checkDirectoryExists(directory);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return sizeOfDirectory0(directory);
    }

    private static long sizeOf0(final File file) {
        if (file.isDirectory()) {
            return sizeOfDirectory0(file);
        }

        return file.length(); // will be 0 if file does not exist
    }

    private static long sizeOfDirectory0(final File directory) {
        // Note: the public entry points take a considerNonExisting*AsEmpty flag, but it is answered before the walk
        // starts (a missing root either yields 0 or raises FileNotFoundException). Every entry reached from here was
        // returned by listFiles(), so it exists; the flag has no meaning below this point and is not threaded through.
        final File[] files = directory.listFiles();

        if (files == null) { // null if security restricted
            return 0L;
        }

        long size = 0;

        for (final File file : files) {
            if (!isSymbolicLinkOrJunction(file)) {
                final long entrySize = sizeOf0(file); // internal method

                // A nested walk that overflowed hands back the negative sentinel. Adding that to a positive running
                // sum could make the total positive again - a parent holding a 100-byte file and a subdirectory
                // that reported -2 used to answer 98 - turning "unmeasurable" into a plausible wrong number. Hand
                // the sentinel back unchanged instead, exactly as this level's own overflow is handed back below.
                if (entrySize < 0) {
                    return entrySize;
                }

                size += entrySize;

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File largeFile = new File("very_large_file.dat");
     * BigInteger size = IOUtil.sizeOfAsBigInteger(largeFile);
     * }</pre>
     *
     * <p>This is the overflow-free counterpart of {@link #sizeOf(File)} and reports exactly the same exceptions
     * for the same inputs.
     *
     * @param file the file or directory whose size is to be calculated, must not be {@code null}.
     * @return the total size as a BigInteger. For directories, this is the recursive sum of all files within it,
     *         with symbolic links found during the walk skipped; a link handed in directly is followed, exactly
     *         as {@link #sizeOf(File)} does.
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     * @throws UncheckedIOException if the file does not exist or is not readable.
     * @see #sizeOf(File)
     * @see #sizeOfDirectoryAsBigInteger(File)
     */
    public static BigInteger sizeOfAsBigInteger(final File file) throws IllegalArgumentException, UncheckedIOException {
        try {
            checkFileExists(file, true);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        if (file.isDirectory()) {
            // The private walker, not the public method: the argument has already been validated above, and
            // re-entering sizeOfDirectoryAsBigInteger(..) would stat it a second time.
            return sizeOfDirectoryAsBigInteger0(file);
        }

        return BigInteger.valueOf(file.length());
    }

    /**
     * Returns the size of the specified directory as a BigInteger.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File largeDir = new File("large_directory");
     * BigInteger totalSize = IOUtil.sizeOfDirectoryAsBigInteger(largeDir);
     * }</pre>
     *
     * <p>This is the overflow-free counterpart of {@link #sizeOfDirectory(File)} and reports exactly the same
     * exceptions for the same inputs: an absent path is a {@link FileNotFoundException}, while a path that exists
     * but is not a directory is an {@link IllegalArgumentException}.
     *
     * @param directory the directory whose size is to be calculated, must not be {@code null}.
     * @return the total size as a BigInteger of all files within the directory and its subdirectories, with
     *         symbolic links found during the walk skipped and an unreadable subdirectory contributing nothing.
     * @throws IllegalArgumentException if {@code directory} is {@code null}, or exists but is not a directory.
     * @throws UncheckedIOException if the directory does not exist or cannot be read.
     * @see #sizeOfDirectory(File)
     * @see #sizeOfAsBigInteger(File)
     */
    public static BigInteger sizeOfDirectoryAsBigInteger(final File directory) throws IllegalArgumentException, UncheckedIOException {
        try {
            checkDirectoryExists(directory);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

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
            if (!isSymbolicLinkOrJunction(file)) {
                size = size.add(sizeOfAsBigInteger0(file)); // internal method
            }
        }

        return size;
    }

    static File checkFileExists(final File file) throws FileNotFoundException {
        return checkFileExists(file, false, cs.file);
    }

    /**
     * Validates a path argument the caller knows by a name other than {@code "file"}, so that a rejection names
     * the argument the caller actually passed rather than this helper's own parameter. A method taking two
     * {@code File}s otherwise reports both of them as {@code 'file'}.
     *
     * @param file the path to validate.
     * @param argName the caller's name for it, used in the {@code IllegalArgumentException} message.
     * @return {@code file}.
     * @throws IllegalArgumentException if {@code file} is {@code null}, or exists but is a directory.
     * @throws FileNotFoundException if {@code file} does not exist, cannot be read, or is neither a file nor a
     *         directory.
     */
    static File checkFileExists(final File file, final String argName) throws IllegalArgumentException, FileNotFoundException {
        return checkFileExists(file, false, argName);
    }

    /**
     * Renders a file for an error message. Always the absolute path, so a failure reported for a relative
     * {@code File} still identifies which file on disk was meant; {@code null} is rendered as {@code "null"}
     * rather than dereferenced.
     */
    static String describe(final File file) {
        return file == null ? "null" : file.getAbsolutePath();
    }

    static File checkFileExists(final File file, final boolean canBeDirectory) throws FileNotFoundException {
        return checkFileExists(file, canBeDirectory, cs.file);
    }

    /**
     * @throws IllegalArgumentException if {@code file} is {@code null}, or directories are disallowed and {@code file} is a directory.
     * @throws FileNotFoundException if the path does not exist, is unreadable, or is neither a file nor a directory.
     */
    static File checkFileExists(final File file, final boolean canBeDirectory, final String argName) throws IllegalArgumentException, FileNotFoundException {
        N.checkArgNotNull(file, argName);

        if (!file.exists()) {
            throw new FileNotFoundException("'" + describe(file) + "' does not exist");
        }

        if (!file.canRead()) {
            // Same exception type as the absent case - FileInputStream reports a permission denial as
            // FileNotFoundException too - but a message that names which of the two actually happened.
            throw new FileNotFoundException("'" + describe(file) + "' exists but cannot be read");
        }

        if (!file.isFile()) {
            if (file.isDirectory()) {
                if (!canBeDirectory) {
                    throw new IllegalArgumentException("'" + describe(file) + "' is not a file");
                }
            } else {
                // Reached only for something that exists but is neither a regular file nor a directory - a device
                // node, FIFO, socket - or a symbolic link to one: exists() follows a link, so a link to a file or a
                // directory was classified above and a dangling link failed the exists() check at the top. A link
                // to a FIFO used to be let through here and then hung copyFile/copyToDirectory in open() for ever
                // (and gave zip an empty archive); it is what it resolves to.
                throw new FileNotFoundException("'" + describe(file) + "' exists but is neither a file nor a directory");
            }
        }

        return file;
    }

    static void checkDirectoryExists(final File directory) throws FileNotFoundException {
        checkDirectoryExists(directory, cs.directory);
    }

    /**
     * Validates a directory argument the caller knows by a name other than {@code "directory"}; see
     * {@link #checkFileExists(File, String)} for why the name is threaded through.
     *
     * <p>A directory that exists but cannot be read is rejected exactly as {@link #checkFileExists(File, boolean)}
     * rejects one, so that {@code sizeOf(dir)} and {@code sizeOfDirectory(dir)} agree: the former used to throw
     * for an unreadable root while the latter listed nothing and answered {@code 0}, a wrong measurement rather
     * than a reported one. Only the argument itself is held to this; an unreadable <i>sub</i>directory met during
     * a walk still contributes nothing, as documented on the size family.
     *
     * @param directory the directory to validate.
     * @param argName the caller's name for it, used in the {@code IllegalArgumentException} message.
     * @throws IllegalArgumentException if {@code directory} is {@code null}, or exists but is not a directory.
     * @throws FileNotFoundException if {@code directory} does not exist, or exists but cannot be read.
     */
    static void checkDirectoryExists(final File directory, final String argName) throws IllegalArgumentException, FileNotFoundException {
        N.checkArgNotNull(directory, argName);

        if (!directory.exists()) {
            throw new FileNotFoundException("'" + describe(directory) + "' does not exist");
        }

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(directory) + "' is not a directory");
        }

        if (!directory.canRead()) {
            throw new FileNotFoundException("'" + describe(directory) + "' exists but cannot be read");
        }
    }

    /**
     * Validates that {@code destDir} can serve as a destination directory, <b>without</b> creating anything.
     *
     * <p>Kept separate from {@link #createDestDirectory(File)} so that callers can finish validating all of their
     * arguments before any directory is created on disk; otherwise a call that is ultimately rejected still leaves
     * a freshly created directory behind - possibly inside the very source tree it refused to copy.
     *
     * @param destDir the candidate destination directory.
     * @throws IllegalArgumentException if {@code destDir} is {@code null}, or exists but is not a directory.
     */
    static void checkDestDirectory(final File destDir) throws IllegalArgumentException {
        if (destDir == null) {
            throw new IllegalArgumentException("The specified destination directory is null.");
        }

        if (destDir.exists() && !destDir.isDirectory()) {
            throw new IllegalArgumentException("Destination '" + describe(destDir) + "' is not a directory");
        }
    }

    /**
     * Validates {@code destDir} with {@link #checkDestDirectory(File)} and then creates it, along with any missing
     * parent directories, if it does not already exist. Call this only after every other argument has been validated.
     *
     * @param destDir the destination directory to validate and create.
     * @throws IllegalArgumentException if {@code destDir} is {@code null}, or exists but is not a directory.
     * @throws IOException if the directory could not be created or cannot be written to.
     */
    static void createDestDirectory(final File destDir) throws IllegalArgumentException, IOException {
        checkDestDirectory(destDir);

        if (!destDir.exists() && !destDir.mkdirs() && !destDir.isDirectory()) {
            throw new IOException("Failed to create destination directory: " + describe(destDir));
        }

        if (!destDir.canWrite()) {
            throw new IOException("Destination '" + describe(destDir) + "' cannot be written to"); //NOSONAR
        }
    }

    /**
     * Compresses the specified source file or directory and writes the compressed data to the target file using the ZIP compression algorithm.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File sourceDir = new File("documents");
     * File zipFile = new File("documents.zip");
     * IOUtil.zip(sourceDir, zipFile);
     * }</pre>
     *
     * <p>Entry timestamps and symbolic links are handled as {@link #zip(File, File, Charset)} describes.
     *
     * @param sourceFile the file or directory to be compressed.
     * @param targetFile the file to which the compressed data will be written. It is created if it does not exist, and overwritten if it does.
     * @throws IllegalArgumentException if {@code sourceFile} or {@code targetFile} is {@code null}, or if the two denote the same file.
     * @throws FileNotFoundException if the source file does not exist or is not readable.
     * @throws IOException if reading the source files or creating, writing, or closing the ZIP archive {@code targetFile} fails
     * @see #unzip(File, File)
     * @see #zip(File, File, Charset)
     */
    public static void zip(final File sourceFile, final File targetFile) throws IllegalArgumentException, IOException {
        zip(sourceFile, targetFile, null);
    }

    /**
     * Compresses the specified source file or directory into the target file, encoding the entry names with the
     * given charset.
     *
     * <p>Entry names are the only thing the charset affects; entry <i>content</i> is copied byte for byte.
     * Use this when the archive has to be read by a tool that does not expect UTF-8 names - or, in
     * {@link #unzip(File, File, Charset)}, when reading one that was written that way.
     *
     * <p><b>Timestamps:</b> each entry carries its source's last-modified time, whether the source is a single
     * file or one reached by walking a directory, so {@link #unzip(File, File, Charset)} can restore it - but
     * only <b>to whole-second resolution</b>, which is all the ZIP extended-timestamp field records. A round
     * trip through an archive is therefore not exact the way {@link #copyFile(File, File)} is: a source
     * stamped {@code 1600000001234} comes back as {@code 1600000001000}.
     * No other metadata is preserved: permissions, ownership and creation/access times are not recorded.
     *
     * <p><b>Symbolic links:</b> a ZIP archive cannot hold one, so a link inside a directory source is either
     * dereferenced or left out. A link to a regular file is archived as that file's content, stamped with the
     * target's time; a link to a directory is skipped and never descended into (as {@code listFiles},
     * {@code walk} and the delete family never descend into one), and so is a dangling link. The top-level
     * source itself is the one link that is followed, as {@link #copyToDirectory(File, File)} follows one, and its
     * entries are named under the link's own name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOUtil.zip(new File("documents"), new File("documents.zip"), Charset.forName("Shift_JIS"));
     * }</pre>
     *
     * @param sourceFile the file or directory to be compressed.
     * @param targetFile the file to which the compressed data will be written. It is created if it does not exist, and overwritten if it does.
     * @param charset the charset used to encode the ZIP entry names; {@code null} means UTF-8.
     * @throws IllegalArgumentException if {@code sourceFile} or {@code targetFile} is {@code null}, or if the two denote the same file.
     * @throws FileNotFoundException if the source file does not exist or is not readable.
     * @throws IOException if reading the source files or creating, writing, or closing the ZIP archive {@code targetFile} fails, or if
     *         {@code charset} cannot encode an entry name (UTF-8 cannot encode a name holding a lone surrogate). An <i>existing</i>
     *         {@code targetFile} is checked before it is opened and so is left unchanged; a {@code targetFile} that did not exist yet is
     *         reported by the write itself and may be left behind partially written.
     * @see #unzip(File, File, Charset)
     */
    public static void zip(final File sourceFile, final File targetFile, final Charset charset) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);

        // Validate the source BEFORE opening the target: openFileOutputStream creates/truncates
        // the target file, which would destroy an existing target when the source is invalid.
        checkFileExists(sourceFile, true, cs.sourceFile);

        // Reject writing the archive onto its own source (same path or hard-link alias): opening the
        // target truncates it, destroying the source before it can be read.
        requireCanonicalPathsNotEquals(sourceFile, targetFile);

        // Only an EXISTING target is worth a second full walk of the source: it is the one that has something to
        // lose. A target that does not exist yet is reported by the write itself, translated below.
        if (targetFile.exists()) {
            checkZipEntryNamesEncodable(sourceFile, targetFile, charset);
        }

        // The target stream is its own resource, not an argument of the ZipOutputStream's: ZipOutputStream.close()
        // writes the central directory first and skips closing what it wraps when that write fails, so the file
        // handle survived every failing close (see zipSingleFile for the same ordering rule).
        try (FileOutputStream fos = openFileOutputStream(targetFile);
             ZipOutputStream zos = newZipOutputStream(fos, charset)) {
            // The translation below covers the WRITE only. On the try-with-resources statement it also covered the
            // resource expression, where openFileOutputStream reports a wrong-kind target (a directory) as an
            // IllegalArgumentException: that was rethrown as "Zip entry name cannot be encoded ..", the wrong type
            // and a cause that never happened.
            try {
                zipFile(sourceFile, zos, targetFile);
            } catch (final IllegalArgumentException e) {
                throw unencodableZipEntryName(e, sourceFile, charset);
            }
        }
    }

    /**
     * Rejects, before the target is opened, an entry name {@code charset} cannot encode. {@code ZipOutputStream}
     * reports one as an {@code IllegalArgumentException} from {@code putNextEntry} - by which point the target had
     * been truncated, so an existing archive was destroyed by a name it could never have held. UTF-8 is checked
     * too: it cannot encode a name holding a lone surrogate, which NTFS accepts.
     *
     * <p>A directory source costs a whole extra walk here, so {@code zip(File, File, Charset)} pays it only when
     * the target already exists; {@code zip(Collection, File, Charset)} rides the walk its duplicate-name check
     * needs anyway.
     */
    private static void checkZipEntryNamesEncodable(final File sourceFile, final File targetFile, final Charset charset) throws IOException {
        final Charset effectiveCharset = charset == null ? StandardCharsets.UTF_8 : charset;
        final CharsetEncoder encoder = effectiveCharset.newEncoder();
        final ZipEntryVisitor check = (entryName, path, attrs) -> requireZipEntryNameEncodable(encoder, effectiveCharset, entryName, sourceFile);

        if (sourceFile.isFile()) {
            check.visit(sourceFile.getName(), sourceFile.toPath(), null);
        } else {
            walkZipSource(sourceFile, targetFile, check, check);
        }
    }

    /**
     * Translates the {@code IllegalArgumentException} that {@code ZipOutputStream.putNextEntry} raises for a name
     * the charset cannot encode into the {@code IOException} this class documents for it. It is normally only a
     * target that did not exist that reaches this - an existing one is guarded by
     * {@link #checkZipEntryNamesEncodable(File, File, Charset)} before it is opened, and only a name that appears
     * between that walk and the write can still get here.
     *
     * <p>Apply it to the write alone, never to the statement that opens the target: acquiring the target raises
     * an {@code IllegalArgumentException} of its own for a wrong-kind argument (a directory), which is the
     * caller's answer and must not be dressed up as an unencodable entry name.
     */
    private static IOException unencodableZipEntryName(final IllegalArgumentException cause, final File sourceFile, final Charset charset) {
        final Charset effectiveCharset = charset == null ? StandardCharsets.UTF_8 : charset;

        return new IOException("Zip entry name cannot be encoded in " + effectiveCharset.name() + " from source: " + describe(sourceFile), cause);
    }

    private static void requireZipEntryNameEncodable(final CharsetEncoder encoder, final Charset charset, final String entryName, final File sourceFile)
            throws IOException {
        if (!encoder.canEncode(entryName)) {
            throw new IOException("Zip entry name cannot be encoded in " + charset.name() + ": '" + entryName + "' from source: " + describe(sourceFile));
        }
    }

    /**
     * Compresses the specified source files or directories and writes the compressed data to the target file using the ZIP compression algorithm.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume a.txt contains "aaa" and b.txt contains "bbb".
     * File a = new File("a.txt");
     * File b = new File("b.txt");
     * File zip = new File("files.zip");
     * IOUtil.zip(Arrays.asList(a, b), zip); // writes a ZIP with one entry per source file
     * }</pre>
     *
     * @param sourceFiles the collection of files or directories to be compressed, must not be {@code null}. Each
     *                    element must be an existing file or directory. An empty collection writes an empty archive.
     * @param targetFile  the file to which the compressed data will be written. It is created if it does not exist, and overwritten if it does.
     * @throws IllegalArgumentException if {@code sourceFiles} or {@code targetFile} is {@code null}, if {@code sourceFiles} holds a
     *         {@code null} element, if any source file and {@code targetFile} are the same file, or if two sources would produce the
     *         same ZIP entry name (two files with the same basename, or a file and a directory sharing one - the entries
     *         {@code "x"} and {@code "x/"} land on the same path once extracted; names are compared exactly, so two
     *         differing only in case are distinct entries even where the file system folds them). In those cases an
     *         existing {@code targetFile} is left unchanged.
     * @throws FileNotFoundException if any source file does not exist or is not readable.
     * @throws IOException if reading the source files or creating, writing, or closing the ZIP archive {@code targetFile} fails
     * @see #unzip(File, File)
     */
    public static void zip(final Collection<File> sourceFiles, final File targetFile) throws IllegalArgumentException, IOException {
        zip(sourceFiles, targetFile, null);
    }

    /**
     * Compresses the specified source files or directories into the target file, encoding the entry names with
     * the given charset.
     *
     * <p>Entry names are the only thing the charset affects; entry <i>content</i> is copied byte for byte. Entry
     * timestamps and symbolic links are handled as {@link #zip(File, File, Charset)} describes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOUtil.zip(Arrays.asList(a, b), new File("files.zip"), Charset.forName("Shift_JIS"));
     * }</pre>
     *
     * @param sourceFiles the collection of files or directories to be compressed, must not be {@code null}. Each
     *                    element must be an existing file or directory. An empty collection writes an empty archive.
     * @param targetFile  the file to which the compressed data will be written. It is created if it does not exist, and overwritten if it does.
     * @param charset the charset used to encode the ZIP entry names; {@code null} means UTF-8.
     * @throws IllegalArgumentException if {@code sourceFiles} or {@code targetFile} is {@code null}, if {@code sourceFiles} holds a
     *         {@code null} element, if any source file and {@code targetFile} are the same file, or if two sources would produce the
     *         same ZIP entry name (a file and a directory sharing a name count as the same; names are compared
     *         exactly, case included). In those cases an existing {@code targetFile} is left unchanged.
     * @throws FileNotFoundException if any source file does not exist or is not readable.
     * @throws IOException if reading the source files or creating, writing, or closing the ZIP archive {@code targetFile} fails, or if
     *         {@code charset} cannot encode an entry name (UTF-8 cannot encode a name holding a lone surrogate) - detected before the target is
     *         opened, so an existing {@code targetFile} is left unchanged.
     * @see #unzip(File, File, Charset)
     */
    public static void zip(final Collection<File> sourceFiles, final File targetFile, final Charset charset) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(targetFile, cs.targetFile);

        // A null collection is a programming error, not "zip nothing": without this check the loops below
        // would throw NullPointerException instead of the documented IllegalArgumentException.
        N.checkArgNotNull(sourceFiles, cs.sourceFiles);

        // Validate all sources BEFORE opening the target: openFileOutputStream creates/truncates
        // the target file, which would destroy an existing target when a source is invalid. A source
        // that IS the target (same path or hard-link alias) is also rejected here - it would be
        // truncated before being read (silent data loss).
        final Set<String> entryNames = new LinkedHashSet<>();

        for (final File sourceFile : sourceFiles) {
            checkFileExists(sourceFile, true, cs.sourceFile);

            requireCanonicalPathsNotEquals(sourceFile, targetFile);
            // One walk per source: the duplicate-name check and the encodability check share it.
            collectZipEntryNames(sourceFile, targetFile, entryNames, charset == null ? StandardCharsets.UTF_8 : charset);
        }

        // See zip(File, File, Charset): the target stream is its own resource so a ZipOutputStream.close() that
        // fails while writing the central directory still cannot leave the file handle open.
        try (FileOutputStream fos = openFileOutputStream(targetFile);
             ZipOutputStream zos = newZipOutputStream(fos, charset)) {
            for (final File sourceFile : sourceFiles) {
                zipFile(sourceFile, zos, targetFile);
            }
        }
    }

    /**
     * Compresses the specified source file and writes the compressed data to the target file using the provided ZipOutputStream.
     * This is a helper method used in the process of creating a ZIP file.
     *
     * @param sourceFile the file to be compressed. This must be a valid file.
     * @param zos        the ZipOutputStream to which the compressed data will be written.
     * @param targetFile the file to which the compressed data will be written. This must be a valid file.
     * @throws IOException if traversing {@code sourceFile} or writing its entries to {@code zos} fails
     */
    private static void zipFile(final File sourceFile, final ZipOutputStream zos, final File targetFile) throws IOException {
        if (sourceFile.isFile()) {
            zipSingleFile(sourceFile, zos);
        } else {
            walkZipSource(sourceFile, targetFile, (entryName, dir, attrs) -> {
                zos.putNextEntry(newZipEntry(entryName, attrs));
                zos.closeEntry();
            }, (entryName, file, attrs) -> {
                zos.putNextEntry(newZipEntry(entryName, attrs));
                Files.copy(file, zos);
                zos.closeEntry();
            });
        }
    }

    /**
     * What {@link #walkZipSource(File, File, ZipEntryVisitor, ZipEntryVisitor)} reports for each entry a directory
     * source contributes to an archive.
     */
    @FunctionalInterface
    private interface ZipEntryVisitor {
        /**
         * @param entryName the ZIP entry name, {@code '/'}-separated and ending in {@code '/'} for a directory.
         * @param path      the file or directory on disk.
         * @param attrs     the attributes to stamp the entry with - for a symbolic link, those of its target.
         * @throws IOException if writing or recording the entry fails.
         */
        void visit(String entryName, Path path, BasicFileAttributes attrs) throws IOException;
    }

    /**
     * Walks a directory source exactly once for both passes of {@code zip(..)} - the name-collision check and the
     * write - so the two can never disagree about what the archive holds.
     *
     * <p><b>Symbolic links.</b> The walk does not follow links, and {@link ZipOutputStream} cannot represent one,
     * so a link is either dereferenced or left out: a link to a regular file is archived as that file's content,
     * stamped with the <i>target's</i> time; a link to a directory is skipped, and so is a dangling one. Handing a
     * directory link to {@code Files.copy(link, zos)}, as the walk used to, followed it into a directory open and
     * failed part-way through the archive - for a tree that {@code copyToDirectory} copies without complaint.
     * Descending into directory links instead would need cycle detection the rest of this class deliberately does
     * without ({@code listFiles}, {@code walk} and the delete family never descend into one either).
     *
     * <p>The <i>top-level</i> source is the one exception, and it is followed, as {@code copyToDirectory} follows
     * a top-level directory link: {@code walkFileTree} would otherwise hand the link itself to {@code visitFile}
     * and archive nothing. Entries are named under the link's own name, not the target's.
     *
     * <p>Entry names are relative to the source's parent, so they start with the source directory's own name; a
     * source that is a filesystem root has no name, its entries are relative to the root itself, and the root
     * gets no directory entry of its own (it would be named {@code "/"}).
     *
     * @param sourceFile  the directory (or link to one) being archived.
     * @param targetFile  the archive being written, which is excluded from its own contents.
     * @param onDirectory called for every directory, the source itself first.
     * @param onFile      called for every regular file, links to one included.
     * @throws IOException if the walk or a visitor fails.
     */
    private static void walkZipSource(final File sourceFile, final File targetFile, final ZipEntryVisitor onDirectory, final ZipEntryVisitor onFile)
            throws IOException {
        final Path sourcePath = sourceFile.toPath().toAbsolutePath().normalize();
        final Path walkRoot = Files.isSymbolicLink(sourcePath) ? sourcePath.toRealPath() : sourcePath;
        final Path sourceName = sourcePath.getFileName();
        final String rootName = sourceName == null ? "" : sourceName.toString();
        final Path normalizedTargetPath = targetFile.toPath().toAbsolutePath().normalize();
        final boolean targetExists = Files.exists(normalizedTargetPath);

        Files.walkFileTree(walkRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                // A nested link to a directory is left out of the archive (visitFile does the same for a symbolic
                // link). A Windows junction is not a symbolic link to walkFileTree, which descends into it as if it
                // were a plain directory, so it is recognised here and its subtree skipped.
                if (!dir.equals(walkRoot) && isSymbolicLinkOrJunction(dir, attrs)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                final String entryName = zipEntryName(walkRoot, rootName, dir);

                // A filesystem root has no name, so its own entry would be "" + "/" - an absolute entry name that
                // unzip(..), and most other extractors, reject. Its children are still named relative to it; only
                // the nameless root itself is left out.
                if (!entryName.isEmpty()) {
                    onDirectory.visit(entryName + "/", dir, attrs);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                BasicFileAttributes entryAttrs = attrs;

                // Classified BEFORE the archive-target test: that test may consult Files.isSameFile(..), which follows
                // a link and throws NoSuchFileException for a dangling one - so a dangling link has to be recognised
                // and skipped here first.
                if (attrs.isSymbolicLink()) {
                    // The walk handed us the link's own attributes. Look through it: a dangling link has no target
                    // to archive, a directory link is not descended into (see above), and a file link is archived
                    // as its target, dated as its target. A link that cannot be looked through at all - one that
                    // loops back on itself (ELOOP), for which the platform raises a FileSystemException other than
                    // NoSuchFileException - has no target either and is left out the same way.
                    try {
                        entryAttrs = Files.readAttributes(file, BasicFileAttributes.class);
                    } catch (final java.nio.file.FileSystemException e) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (!entryAttrs.isRegularFile()) {
                        return FileVisitResult.CONTINUE;
                    }
                } else if (!attrs.isRegularFile()) {
                    // A FIFO, socket or device node: an archive cannot hold one, and reading a FIFO blocks until a
                    // writer appears - which used to hang zip for ever, after the target had been truncated.
                    return FileVisitResult.CONTINUE;
                }

                if (isArchiveTarget(file, normalizedTargetPath, targetExists)) {
                    return FileVisitResult.CONTINUE;
                }

                onFile.visit(zipEntryName(walkRoot, rootName, file), file, entryAttrs);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                // A dangling junction fails before preVisitDirectory is ever called (the walker cannot open it), so
                // the "a nested directory link is left out" rule has to be applied here: an entry that is a link of
                // any kind and cannot be read is left out, exactly like a dangling symbolic link in visitFile.
                final BasicFileAttributes attrs;

                try {
                    attrs = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                } catch (final IOException e) {
                    exc.addSuppressed(e);
                    throw exc;
                }

                if (isSymbolicLinkOrJunction(file, attrs)) {
                    return FileVisitResult.CONTINUE;
                }

                throw exc;
            }
        });
    }

    /**
     * Names an entry of a directory source: the source's own name, then the path below it, {@code '/'}-separated.
     *
     * @param walkRoot the directory the walk started from (the source, or its target when the source is a link).
     * @param rootName the source's own name, or empty for a filesystem root.
     * @param path     the file or directory being named.
     * @return the entry name, without a trailing {@code '/'}.
     */
    private static String zipEntryName(final Path walkRoot, final String rootName, final Path path) {
        final String relative = walkRoot.relativize(path).toString().replace('\\', '/');

        if (rootName.isEmpty()) {
            return relative;
        }

        return relative.isEmpty() ? rootName : rootName + "/" + relative;
    }

    /**
     * Compresses a single regular file and writes the compressed data to the target file using the provided ZipOutputStream.
     * This is a helper method used in the process of creating a ZIP file; directory sources are
     * handled by the {@code Files.walkFileTree} branch of the caller.
     *
     * @param file the file to be compressed. This must be a valid file.
     * @param zos  the ZipOutputStream to which the compressed data will be written.
     * @throws IOException if reading {@code file} or writing its ZIP entry to {@code zos} fails.
     */
    private static void zipSingleFile(final File file, final ZipOutputStream zos) throws IOException {
        // No "is this the archive itself?" check here: every public zip(..) entry point already rejects a source
        // that resolves to the target via requireCanonicalPathsNotEquals, so the old guard could never fire - and
        // had it fired it would have silently dropped a file from the archive instead of reporting anything. The
        // archive-inside-the-source-directory case is handled by isArchiveTarget during the directory walk.
        final String relativeFileName = file.getName();

        final ZipEntry ze = new ZipEntry(relativeFileName);
        // Deliberately no ze.setSize(file.length()): the size is only required for STORED entries, and declaring it
        // for a DEFLATED entry makes ZipOutputStream verify the byte count, so a file that changes size while it is
        // being zipped fails with "invalid entry size" instead of simply archiving whatever was there.
        ze.setLastModifiedTime(FileTime.fromMillis(file.lastModified()));
        zos.putNextEntry(ze);

        // Allocate the buffer before opening the stream (and open the stream inside the try), so a
        // failure in either step cannot leak the other resource - same ordering as split()/merge().
        final byte[] buf = Objectory.createByteArrayBuffer();

        try (InputStream is = openFileInputStream(file)) {
            int count = 0;

            while (EOF != (count = read(is, buf, 0, buf.length))) {
                if (count == 0) {
                    break;
                }

                zos.write(buf, 0, count);
            }
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Applies a ZIP entry's recorded last-modified time to the file just extracted from it, so that a
     * {@code zip} / {@code unzip} round trip carries timestamps across the way {@code copyFile} and
     * {@code copyToDirectory} already do.
     *
     * <p>An entry carrying no time at all is left alone rather than stamped with an invented one.
     *
     * @param entry the entry that was extracted.
     * @param path  the file it was extracted to.
     */
    private static void restoreEntryTime(final ZipEntry entry, final Path path) {
        applyEntryTimeQuietly(path, entry.getLastModifiedTime());
    }

    /**
     * Applies a recorded last-modified time to a path just extracted from an archive, <b>best effort</b>.
     *
     * <p>Escalating a failure here would abort an extraction whose content has already been written, leaving
     * the archive half-unpacked over a stamp the caller has no way to switch off: unlike {@code copyFile} and
     * {@code copyToDirectory}, {@code unzip} has no {@code preserveFileDate} flag. The failure is logged
     * instead, and the entry keeps whatever time it got when it was written.
     *
     * @param path the file or directory that was extracted.
     * @param time the time to apply; nothing is done when it is {@code null}.
     */
    private static void applyEntryTimeQuietly(final Path path, final FileTime time) {
        if (time == null) {
            return;
        }

        try {
            Files.setLastModifiedTime(path, time);
        } catch (final IOException e) {
            logger.warn(e, "Failed to restore the last-modified time of the extracted entry: {}", path);
        }
    }

    /**
     * Records a directory entry's last-modified time for {@link #unzip(File, File, Charset)} to apply once the
     * whole archive has been extracted. See the call site for why it cannot be applied immediately.
     *
     * @param entry     the directory entry.
     * @param path      the directory it was extracted to.
     * @param collected the list to append to.
     */
    private static void collectEntryTime(final ZipEntry entry, final Path path, final List<Pair<Path, FileTime>> collected) {
        final FileTime time = entry.getLastModifiedTime();

        if (time != null) {
            collected.add(Pair.of(path, time));
        }
    }

    /**
     * Builds a ZIP entry carrying the source's last-modified time.
     *
     * <p>Without the explicit stamp {@link ZipOutputStream} dates the entry "now", so an archive built from a
     * directory used to lose every timestamp while an archive built from a single file kept them - the same
     * public {@code zip(..)} call answering differently depending on the kind of source it was given.
     *
     * <p>{@link ZipEntry#setLastModifiedTime(FileTime)} rather than {@link ZipEntry#setTime(long)}: the latter
     * records only the MS-DOS timestamp, whose resolution is two seconds, so half of all whole-second times
     * would come back off by one second.
     *
     * @param entryName the ZIP entry name.
     * @param attrs     the source's attributes, already read by the file-tree walk.
     * @return the entry, stamped with {@code attrs.lastModifiedTime()}.
     */
    private static ZipEntry newZipEntry(final String entryName, final BasicFileAttributes attrs) {
        final ZipEntry entry = new ZipEntry(entryName);
        entry.setLastModifiedTime(attrs.lastModifiedTime());

        return entry;
    }

    private static void collectZipEntryNames(final File sourceFile, final File targetFile, final Set<String> names, final Charset charset) throws IOException {
        final CharsetEncoder encoder = charset.newEncoder();

        if (sourceFile.isFile()) {
            // No "is this the archive itself?" check: the caller has already rejected that case with
            // requireCanonicalPathsNotEquals, and skipping here would have to be mirrored in zipSingleFile to
            // stay consistent. Directory sources still need isArchiveTarget below, where the archive can
            // legitimately sit inside the tree being zipped.
            requireZipEntryNameEncodable(encoder, charset, sourceFile.getName(), sourceFile);
            addZipEntryName(names, sourceFile.getName(), sourceFile);
            return;
        }

        // The same walk the writing pass uses, so a name this pass accepts is exactly a name that pass will write.
        final ZipEntryVisitor record = (entryName, path, attrs) -> {
            requireZipEntryNameEncodable(encoder, charset, entryName, sourceFile);
            addZipEntryName(names, entryName, sourceFile);
        };

        walkZipSource(sourceFile, targetFile, record, record);
    }

    private static void addZipEntryName(final Set<String> names, final String name, final File sourceFile) {
        // Compared without the directory marker: a regular file "x" and a directory "x" from two sources produce
        // the entries "x" and "x/", which are distinct strings but the same path once extracted. Letting both in
        // wrote an archive that unzip(..) - or any extractor - cannot unpack, since the second entry lands on the
        // first, and the caller learned that only at extraction time instead of here, where the sources are.
        final String path = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;

        if (!names.add(path)) {
            throw new IllegalArgumentException("Duplicate ZIP entry name '" + name + "' from source: " + describe(sourceFile));
        }
    }

    /**
     * Checks whether {@code file} denotes the archive currently being written, which must be excluded from
     * the archive's own contents rather than added to it.
     *
     * <p>Distinct paths can still resolve to the same file through a hard link, but
     * {@link Files#isSameFile(Path, Path)} requires both paths to exist. It is therefore consulted only when
     * the target already exists; otherwise zipping a directory to a new archive - the common case - would
     * fail with {@link java.nio.file.NoSuchFileException}. {@code targetExists} is passed in rather than
     * tested here so that the traversal does not stat the target once per visited file.
     *
     * @param file the file visited while traversing a source directory.
     * @param normalizedTargetPath the absolute, normalized path of the archive being written.
     * @param targetExists whether {@code normalizedTargetPath} existed when the traversal started.
     * @return {@code true} if {@code file} is the archive being written.
     * @throws IOException if checking whether {@code file} and {@code normalizedTargetPath} identify the same existing file fails
     */
    private static boolean isArchiveTarget(final Path file, final Path normalizedTargetPath, final boolean targetExists) throws IOException {
        return normalizedTargetPath.equals(file.toAbsolutePath().normalize()) || (targetExists && Files.isSameFile(file, normalizedTargetPath));
    }

    /**
     * The entry's destination as a {@link Path}. {@code File.toPath()} throws {@link InvalidPathException} - an
     * unchecked {@code IllegalArgumentException} - for a name the platform cannot spell (a Unix-made archive holding
     * {@code log-12:30.txt} or {@code a|b} extracted on Windows), and the method's contract reserves
     * {@code IllegalArgumentException} for its arguments: a malformed entry is an {@code IOException}.
     */
    private static Path entryPath(final File newFile, final String entryName) throws IOException {
        try {
            return newFile.toPath();
        } catch (final InvalidPathException e) {
            throw new IOException("Zip entry is not a valid file name on this platform: '" + entryName + "' (" + e.getMessage() + ")", e);
        }
    }

    /**
     * The second half of {@code unzip}'s traversal guard: the deepest ancestor of {@code newFile} that already
     * exists is resolved through every link and junction ({@link Path#toRealPath}) and must lie under the real
     * path of the target directory. The first half compares canonical paths, and whether
     * {@link File#getCanonicalPath()} resolves symbolic links or junctions on Windows depends on the JDK (observed
     * both ways), so an archive extracted into a directory that already holds a link pointing elsewhere could write
     * through it if that were the only guard.
     *
     * @param newPath        the entry's destination, already canonicalised.
     * @param targetRealPath the target directory's real path.
     * @param entryName      the entry, for the message.
     * @throws IOException if the destination resolves outside the target directory.
     */
    private static void requireInsideTarget(final Path newPath, final Path targetRealPath, final String entryName) throws IOException {
        Path probe = newPath;

        while (probe != null && !Files.exists(probe, LinkOption.NOFOLLOW_LINKS)) {
            probe = probe.getParent();
        }

        if (probe == null) {
            return;
        }

        final Path real;

        try {
            real = probe.toRealPath();
        } catch (final IOException e) {
            // The deepest existing entry is a link that cannot be resolved (a dangling one): nothing written
            // through it can be shown to land inside the target directory.
            throw new IOException("Zip entry's path passes through a link that cannot be resolved: '" + entryName + "' (" + e.getMessage() + ")", e);
        }

        if (!real.startsWith(targetRealPath)) {
            throw new IOException("Zip entry is outside of the target dir (its path passes through a link): " + entryName);
        }
    }

    /**
     * Rejects, on Windows, an entry with a component that ends in a dot or a space. The platform folds a trailing
     * dot away silently - {@code x.txt.} became {@code x.txt} and overwrote an entry the archive never held, and
     * {@code a/...} became {@code a}, a directory created for a file that could not then be opened - and refuses a
     * trailing space from {@code getCanonicalFile()} ("Invalid file path"). Both are reported as the malformed
     * entry they are, with the entry's name, before anything is created. The components {@code .} and {@code ..}
     * are not names and are left to the containment checks.
     */
    private static void requireSpellableOnThisPlatform(final String platformEntryName, final String entryName) throws IOException {
        if (!IS_OS_WINDOWS) {
            return;
        }

        for (final String component : Strings.split(platformEntryName, File.separatorChar)) {
            if (!component.isEmpty() && !".".equals(component) && !"..".equals(component) && (component.endsWith(".") || component.endsWith(" "))) {
                throw new IOException("Zip entry is not a valid file name on this platform (a component ends in a dot or a space): '" + entryName + "'");
            }
        }
    }

    /**
     * {@code platformEntryName} under {@code targetDir}, canonicalised. A name the platform rejects outright
     * ({@code File.getCanonicalFile()} throws "Invalid file path" for a Windows name ending in a dot or a space) is
     * reported with the entry, as {@link #entryPath(File, String)} reports one {@code File.toPath()} rejects.
     */
    private static File canonicalEntryFile(final File targetDir, final String platformEntryName, final String entryName) throws IOException {
        try {
            return new File(targetDir, platformEntryName).getCanonicalFile();
        } catch (final IOException e) {
            throw new IOException("Zip entry is not a valid file name on this platform: '" + entryName + "' (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Unzips the specified source ZIP file to the target directory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File zipFile = new File("documents.zip");
     * File targetDir = new File("extracted");
     * IOUtil.unzip(zipFile, targetDir);
     * }</pre>
     *
     * <p><b>Existing files are overwritten without warning.</b> Unlike {@link #copyToDirectory(File, File)}, which
     * refuses to replace anything, extraction truncates and rewrites any file already at an entry's path.
     * Extract into a fresh or empty directory if that matters. Extraction is also not transactional: a failure
     * part-way through leaves the entries written so far in place.
     *
     * <p><b>Note:</b> extraction is guarded against path traversal (entry names that are absolute or that would
     * resolve outside {@code targetDir} are rejected, as is an entry that would overwrite the source archive), but it
     * is <i>not</i> bounded: there is no limit on the entry count or on the expansion ratio, so a hostile archive can
     * still fill the target volume. Only extract archives you trust, or check {@code ZipEntry.getSize()} yourself
     * first via {@link ZipFile}.
     *
     * <p><b>Timestamps:</b> every extracted file and directory is stamped with the last-modified time its
     * entry carries, so a {@code zip} / {@code unzip} round trip carries dates across the way
     * {@code copyFile} and {@code copyToDirectory} do - but only <b>to whole-second resolution</b>, which is
     * all the ZIP extended-timestamp field records. {@code copyFile} preserves the source's
     * {@link FileTime} exactly; an archive round trip does not, so a stamp of {@code 1600000001234} comes back
     * as {@code 1600000001000}. Directory times are applied after the whole archive has been written, since
     * extracting a file into a directory updates that directory's own time. An entry recording no time is left
     * with the time it got when it was written, and a stamp the filesystem refuses is logged and skipped
     * rather than failing an extraction whose content is already on disk - this method has no
     * {@code preserveFileDate} switch for a caller to turn off. No other metadata is restored: permissions and
     * ownership come from the process's own defaults.
     *
     * @param srcZipFile the source ZIP file to be unzipped. This must be a valid ZIP file.
     * @param targetDir  the directory to which the contents of the ZIP file will be extracted. It is created if it
     *                   does not exist. Files already present at an entry's path are overwritten.
     * @throws IllegalArgumentException if {@code srcZipFile} is {@code null} or a directory, or if {@code targetDir}
     *         is {@code null} or is an existing file.
     * @throws IOException if {@code srcZipFile} does not exist, a ZIP entry is absolute, would be extracted outside
     *         {@code targetDir}, names the target directory itself rather than a file inside it, is a file entry
     *         whose path is already a directory (or a directory entry whose path is already a file), would
     *         overwrite the source archive, or another I/O error occurs during extraction. A malformed entry is
     *         never reported as {@code IllegalArgumentException}: that is reserved for the arguments.
     */
    public static void unzip(final File srcZipFile, final File targetDir) throws IllegalArgumentException, IOException {
        unzip(srcZipFile, targetDir, null);
    }

    /**
     * Unzips the specified source ZIP file to the target directory, decoding the entry names with the given
     * charset.
     *
     * <p>Everything {@link #unzip(File, File)} documents applies here as well - existing files are overwritten,
     * each entry's recorded last-modified time is restored, and extraction is guarded against path traversal but
     * is not bounded in size or entry count. The charset only selects how entry <i>names</i> are decoded, which
     * matters for an archive written by a tool that does not use UTF-8 names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOUtil.unzip(new File("legacy.zip"), new File("extracted"), Charset.forName("Shift_JIS"));
     * }</pre>
     *
     * @param srcZipFile the source ZIP file to be unzipped. This must be a valid ZIP file.
     * @param targetDir  the directory to which the contents of the ZIP file will be extracted. It is created if it
     *                   does not exist. Files already present at an entry's path are overwritten.
     * @param charset the charset used to decode the ZIP entry names; {@code null} means UTF-8.
     * @throws IllegalArgumentException if {@code srcZipFile} is {@code null} or a directory, or if {@code targetDir} is
     *         {@code null} or is an existing file.
     * @throws IOException if {@code srcZipFile} does not exist, a ZIP entry is absolute, would be extracted outside
     *         {@code targetDir}, names the target directory itself rather than a file inside it, is a file entry
     *         whose path is already a directory (or a directory entry whose path is already a file), would
     *         overwrite the source archive, or another I/O error occurs during extraction. A malformed entry is
     *         never reported as {@code IllegalArgumentException}: that is reserved for the arguments.
     * @see #unzip(File, File)
     * @see #zip(File, File, Charset)
     */
    public static void unzip(final File srcZipFile, final File targetDir, final Charset charset) throws IllegalArgumentException, IOException {
        checkFileExists(srcZipFile, cs.srcZipFile);
        createDestDirectory(targetDir);

        final File canonicalTargetDir = targetDir.getCanonicalFile();
        final Path canonicalTargetPath = canonicalTargetDir.toPath();
        final Path targetRealPath = canonicalTargetPath.toRealPath();
        final File canonicalSourceZip = srcZipFile.getCanonicalFile();

        // Directory timestamps are applied only once every entry has been written: extracting a file into a
        // directory updates that directory's own modification time, so restoring it as the entry is met would
        // just be overwritten by the next file to land inside it.
        final List<Pair<Path, FileTime>> directoryTimes = new ArrayList<>();

        // Taken immediately above the try whose finally recycles it, as split()/merge()/zipSingleFile() do:
        // every statement above here can throw, and a buffer that is never handed back costs the pool one
        // entry - pooling efficiency only, nothing external is held.
        final byte[] buf = Objectory.createByteArrayBuffer();
        final int bufLength = buf.length;

        try (ZipFile zip = new ZipFile(srcZipFile, checkCharset(charset))) {
            final Enumeration<? extends ZipEntry> entryEnum = zip.entries();

            while (entryEnum.hasMoreElements()) {
                final ZipEntry ze = entryEnum.nextElement();

                // Fix for Zip Slip
                final String entryName = ze.getName();

                // ZIP entry names are relative. Reject rooted forms explicitly because absolute-path
                // handling differs among File, Path, operating systems, and ZIP-producing tools.
                // Character.isLetter here, not the ASCII-only isDriveLetter(..) used elsewhere: this is a
                // rejection guard, where being more permissive means rejecting more, which is the safe
                // direction. A name is untrusted input and ':' is not a legal Windows path character anyway.
                if (entryName.startsWith("/") || entryName.startsWith("\\")
                        || (entryName.length() > 1 && Character.isLetter(entryName.charAt(0)) && entryName.charAt(1) == ':')) {
                    throw new IOException("Zip entry has an absolute path: " + entryName);
                }

                // Treat both ZIP's standard '/' and the backslash accepted by many ZIP tools as
                // separators on every host. The same File object must be used for validation and
                // extraction; otherwise an entry such as "..\\outside" is a harmless-looking file
                // name on Unix during validation but becomes a parent traversal when normalized later.
                final String platformEntryName = entryName.replace('\\', File.separatorChar).replace('/', File.separatorChar);
                requireSpellableOnThisPlatform(platformEntryName, entryName);
                final File newFile = canonicalEntryFile(canonicalTargetDir, platformEntryName, entryName);
                final Path newPath = entryPath(newFile, entryName);

                if (!newPath.startsWith(canonicalTargetPath)) {
                    throw new IOException("Zip entry is outside of the target dir: " + entryName);
                }

                // File.getCanonicalPath is not relied on to resolve symbolic links or junctions on Windows (whether it
                // does depends on the JDK), so the lexical test above cannot be trusted to see an entry that escapes
                // through a link already present under the target directory (target/j -> elsewhere, entry
                // "j/evil.txt"); the deepest existing ancestor is resolved for real to close that.
                requireInsideTarget(newPath, targetRealPath, entryName);

                if (newFile.equals(canonicalSourceZip) || (newFile.exists() && Files.isSameFile(newPath, srcZipFile.toPath()))) {
                    throw new IOException("Zip entry would overwrite the source archive: " + entryName);
                }

                if (ze.isDirectory()) {
                    // The mirror image of the file-entry check below, so both collisions name the ENTRY at fault
                    // rather than leaving the second to Files.createDirectories' path-only FileAlreadyExistsException.
                    if (newFile.isFile()) {
                        throw new IOException("Zip entry names an existing file: '" + entryName + "'");
                    }

                    Files.createDirectories(newPath);
                    collectEntryTime(ze, newPath, directoryTimes);
                    continue;
                }

                // A file entry whose name resolves to the target directory itself - an empty name, or "." -
                // cannot be extracted. Left to openFileOutputStream it surfaced as a raw platform failure
                // ("... (Access is denied)" on Windows) naming the directory rather than the entry, so the
                // caller could not tell which entry was at fault, nor why.
                if (newFile.equals(canonicalTargetDir) || ".".equals(platformEntryName) || platformEntryName.endsWith(File.separator + ".")) {
                    // "x/." names the directory x, which canonicalisation folded to "x": extracting the entry as a file
                    // called x is not what the archive says.
                    throw new IOException("Zip entry does not name a file inside the target dir: '" + entryName + "'");
                }

                // A file entry landing on an existing directory - "sub/" followed by "sub", or a directory source
                // and a file source of the same name in one zip(Collection, ..) - used to surface as
                // openFileOutputStream's IllegalArgumentException, which is this class's "bad argument" answer.
                // The argument is fine; the ARCHIVE is malformed, and this method promises IOException for that.
                if (newFile.isDirectory()) {
                    throw new IOException("Zip entry names an existing directory: '" + entryName + "'");
                }

                try (InputStream is = zip.getInputStream(ze);
                     OutputStream os = openFileOutputStream(newFile)) {
                    int count = 0;

                    while (EOF != (count = read(is, buf, 0, bufLength))) {
                        if (count == 0) {
                            break;
                        }

                        os.write(buf, 0, count);
                    }

                    os.flush();
                }

                // Restored after the stream is closed: writing the content is what sets the file's time, so
                // stamping it earlier would be undone by the very bytes being extracted.
                restoreEntryTime(ze, newPath);
            }

            for (final Pair<Path, FileTime> directoryTime : directoryTimes) {
                applyEntryTimeQuietly(directoryTime.left(), directoryTime.right());
            }
        } finally {
            Objectory.recycle(buf);
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
     *   <li><strong>Numbering:</strong> Sequential zero-padded numbers starting from 1. The padding width is at least
     *       4 digits, and widens to the digit count of {@code countOfParts} when that is larger, so the part names of
     *       a single split always sort in part order.</li>
     *   <li><strong>Example:</strong> {@code document.pdf} becomes {@code document.pdf_0001}, {@code document.pdf_0002}, etc.</li>
     *   <li><strong>Existing parts:</strong> a file already present under a generated part name is truncated and overwritten.</li>
     * </ul>
     *
     * <p>Size distribution:</p>
     * <ul>
     *   <li><strong>Part size:</strong> Each part is either {@code fileLength / countOfParts} or
     *       {@code fileLength / countOfParts + 1} bytes, so no two parts differ by more than one byte.</li>
     *   <li><strong>Where the remainder goes:</strong> the <i>first</i> {@code fileLength % countOfParts} parts
     *       each take the extra byte - not the last one. With 10 bytes in 4 parts that is 3, 3, 2, 2.</li>
     *   <li><strong>Empty files:</strong> If the source file is empty, one empty part file is created, under the same
     *       suffix width a non-empty split into {@code countOfParts} parts would use ({@code document.pdf_000001} for
     *       a count of 100000)</li>
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
     * @param file the source file to split; must exist and be readable.
     * @param countOfParts the number of parts to split the file into; must be greater than 0.
     * @throws IllegalArgumentException if {@code file} is {@code null} or {@code countOfParts} is less than 1.
     * @throws FileNotFoundException if the source file does not exist.
     * @throws IOException if there are issues with file validation or writing the parts.
     * @see #split(File, int, File)
     * @see #splitBySize(File, long)
     * @see #splitBySize(File, long, File)
     * @see #splitByLine(File, int, File)
     */
    public static void split(final File file, final int countOfParts) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgPositive(countOfParts, cs.countOfParts);

        split(file, countOfParts, file.getAbsoluteFile().getParentFile());
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
     *   <li><strong>Numbering:</strong> Sequential zero-padded numbers starting from 1. The padding width is at least
     *       4 digits, and widens to the digit count of {@code countOfParts} when that is larger, so the part names of
     *       a single split always sort in part order.</li>
     *   <li><strong>Example:</strong> {@code document.pdf} becomes {@code document.pdf_0001}, {@code document.pdf_0002}, etc.</li>
     *   <li><strong>Existing parts:</strong> a file already present under a generated part name is truncated and overwritten.</li>
     * </ul>
     *
     * <p>Size distribution:</p>
     * <ul>
     *   <li><strong>Part size:</strong> Each part is either {@code fileLength / countOfParts} or
     *       {@code fileLength / countOfParts + 1} bytes, so no two parts differ by more than one byte.</li>
     *   <li><strong>Where the remainder goes:</strong> the <i>first</i> {@code fileLength % countOfParts} parts
     *       each take the extra byte - not the last one. With 10 bytes in 4 parts that is 3, 3, 2, 2.</li>
     *   <li><strong>Empty files:</strong> If the source file is empty, one empty part file is created, under the same
     *       suffix width a non-empty split into {@code countOfParts} parts would use ({@code document.pdf_000001} for
     *       a count of 100000)</li>
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
     * @param file the source file to split; must exist and be readable.
     * @param countOfParts the number of parts to split the file into; must be greater than 0.
     * @param destDir the directory where the split parts will be saved; it is created if it does not exist and must be writable.
     * @throws IllegalArgumentException if {@code file} is {@code null}, {@code countOfParts} is less than 1, or
     *         {@code destDir} is {@code null} or is an existing file, or a part path aliases the source file.
     * @throws FileNotFoundException if the source file does not exist or is not readable.
     * @throws IOException if the destination directory cannot be created or written to, or another I/O error occurs.
     * @see #split(File, int)
     * @see #splitBySize(File, long)
     * @see #splitBySize(File, long, File)
     * @see #splitByLine(File, int, File)
     */
    public static void split(final File file, final int countOfParts, final File destDir) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);
        N.checkArgPositive(countOfParts, cs.countOfParts);
        checkFileExists(file);
        // Every other argument is validated above, and createDestDirectory validates before it creates,
        // so a rejected call leaves no new directory behind.
        createDestDirectory(destDir);

        final long fileLen = file.length();

        if (fileLen == 0) {
            // One empty part, under the suffix width a non-empty split into countOfParts would use, so the name
            // sorts alongside such parts; an existing file under that name is truncated, as documented.
            final File part = new File(
                    destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + file.getName() + "_" + Strings.padStart("1", partSuffixLength(countOfParts), '0'));
            requireCanonicalPathsNotEquals(file, part);
            openFileOutputStream(part).close();
            return;
        }

        final long baseSizeOfPart = fileLen / countOfParts;
        final long remainder = fileLen % countOfParts;
        final String fileName = file.getName();
        final int suffixLen = partSuffixLength(countOfParts);
        final byte[] buf = Objectory.createByteArrayBuffer();

        try (InputStream input = openFileInputStream(file)) {
            for (int i = 0; i < countOfParts; i++) {
                final String subFileName = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + fileName + "_"
                        + Strings.padStart(N.stringOf(i + 1), suffixLen, '0');
                long partLength = baseSizeOfPart + (i < remainder ? 1 : 0);

                int count = 0;

                final File part = new File(subFileName);
                requireCanonicalPathsNotEquals(file, part);

                try (OutputStream output = openFileOutputStream(part)) {
                    while (partLength > 0 && EOF != (count = read(input, buf, 0, (int) Math.min(buf.length, partLength)))) {
                        if (count == 0) {
                            break;
                        }

                        output.write(buf, 0, count);

                        partLength = partLength - count;
                    }

                    if (partLength != 0) {
                        throw new IOException("Source file ended before split part " + (i + 1) + " was complete: " + describe(file));
                    }

                    output.flush();
                }
            }
        } finally {
            Objectory.recycle(buf);
        }
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
     *   <li><strong>Numbering:</strong> Sequential zero-padded numbers starting from 1. The padding width is at least
     *       4 digits, and widens to the digit count of the total number of parts when that is larger, so the part names
     *       of a single split always sort in part order.</li>
     *   <li><strong>Example:</strong> {@code document.pdf} becomes {@code document.pdf_0001}, {@code document.pdf_0002}, etc.</li>
     *   <li><strong>Existing parts:</strong> a file already present under a generated part name is truncated and overwritten.</li>
     * </ul>
     *
     * <p>Size distribution:</p>
     * <ul>
     *   <li><strong>Regular parts:</strong> Each part (except the last) will be exactly {@code sizeOfPart} bytes</li>
     *   <li><strong>Last part:</strong> Contains the remaining bytes, which may be smaller than {@code sizeOfPart}</li>
     *   <li><strong>Empty files:</strong> If the source file is empty, one empty part file is created</li>
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
     * @param file the source file to split; must not be {@code null}, must exist and be readable.
     * @param sizeOfPart the maximum size in bytes for each part (except possibly the last part); must be positive.
     * @throws IllegalArgumentException if {@code file} is {@code null} or {@code sizeOfPart} is not positive.
     * @throws FileNotFoundException if {@code file} does not exist or is not readable.
     * @throws IOException if there are issues with file validation or writing the parts.
     * @see #splitBySize(File, long, File)
     * @see #split(File, int, File)
     * @see #split(File, int)
     * @see #splitByLine(File, int, File)
     */
    public static void splitBySize(final File file, final long sizeOfPart) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);

        splitBySize(file, sizeOfPart, file.getAbsoluteFile().getParentFile());
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
     *   <li><strong>Numbering:</strong> Sequential zero-padded numbers starting from 1. The padding width is at least
     *       4 digits, and widens to the digit count of the total number of parts when that is larger, so the part names
     *       of a single split always sort in part order.</li>
     *   <li><strong>Example:</strong> {@code document.pdf} becomes {@code document.pdf_0001}, {@code document.pdf_0002}, etc.</li>
     *   <li><strong>Existing parts:</strong> a file already present under a generated part name is truncated and overwritten.</li>
     * </ul>
     *
     * <p>Size distribution:</p>
     * <ul>
     *   <li><strong>Regular parts:</strong> Each part (except the last) will be exactly {@code sizeOfPart} bytes</li>
     *   <li><strong>Last part:</strong> Contains the remaining bytes, which may be smaller than {@code sizeOfPart}</li>
     *   <li><strong>Empty files:</strong> If the source file is empty, one empty part file is created</li>
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
     * @param file the source file to split; must exist and be readable.
     * @param sizeOfPart the maximum size in bytes for each part (except possibly the last part); must be positive.
     * @param destDir the destination directory where split parts will be saved; it is created if it does not exist and must be writable.
     * @throws IllegalArgumentException if {@code file} or {@code destDir} is {@code null}, if {@code destDir} is an
     *         existing file, if {@code sizeOfPart} is not positive, or a part path aliases the source file.
     * @throws FileNotFoundException if {@code file} does not exist or is not readable.
     * @throws IOException if the destination directory cannot be created or written to, or another I/O error occurs.
     * @see #splitBySize(File, long)
     * @see #split(File, int, File)
     * @see #split(File, int)
     * @see #splitByLine(File, int, File)
     */
    public static void splitBySize(final File file, final long sizeOfPart, final File destDir) throws IllegalArgumentException, IOException {
        // Reject a null source as a bad argument, matching split(File, int, File) and splitBySize(File, long);
        // without this, checkFileExists(null) would report it as a missing file instead.
        N.checkArgNotNull(file, cs.file);
        // The size is validated before the file is looked at, so split(..) and splitBySize(..) report the same
        // exception for the same bad input (IllegalArgumentException for a non-positive size or count, whether or
        // not the file exists), as the class contract promises for the pair.
        N.checkArgPositive(sizeOfPart, cs.sizeOfPart);
        checkFileExists(file);

        // Every other argument is validated first, and createDestDirectory validates before it creates,
        // so a rejected call leaves no new directory behind.
        createDestDirectory(destDir);

        final long fileLength = file.length();
        final long numOfParts = Math.max(1L, (fileLength % sizeOfPart) == 0 ? (fileLength / sizeOfPart) : (fileLength / sizeOfPart) + 1);

        final String fileName = file.getName();
        final int suffixLen = partSuffixLength(numOfParts);
        long fileSerNum = 1;

        final byte[] buf = Objectory.createByteArrayBuffer();

        try (InputStream input = openFileInputStream(file)) {
            for (long i = 0; i < numOfParts; i++) {
                final String subFileName = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + fileName + "_"
                        + Strings.padStart(N.stringOf(fileSerNum++), suffixLen, '0');
                // The last part receives whatever remains — 0 for an empty source file, which still
                // produces its single documented empty part instead of a spurious truncation error.
                final long partStart = i * sizeOfPart;
                long partLength = (i == numOfParts - 1) ? (fileLength - partStart) : sizeOfPart;

                int count = 0;

                final File part = new File(subFileName);
                requireCanonicalPathsNotEquals(file, part);

                try (OutputStream output = openFileOutputStream(part)) {
                    while (partLength > 0 && EOF != (count = read(input, buf, 0, (int) Math.min(buf.length, partLength)))) {
                        if (count == 0) {
                            break;
                        }

                        output.write(buf, 0, count);

                        partLength = partLength - count;
                    }

                    if (partLength != 0) {
                        throw new IOException("Source file ended before split part " + (i + 1) + " was complete: " + describe(file));
                    }

                    output.flush();
                }
            }
        } finally {
            Objectory.recycle(buf);
        }
    }

    /**
     * Returns the zero-padding width for split part names: at least 4 digits (the historical width), widened to the
     * digit count of {@code totalParts} when there are more than 9,999 parts. A fixed width of 4 would make
     * {@code _10000} sort between {@code _1000} and {@code _1001}, so a sorted directory listing would no longer be in
     * part order - and reassembling with {@link #merge(Collection, File)} relies on that order.
     *
     * @param totalParts the total number of parts that will be written; must be positive.
     * @return the number of digits each part number is padded to.
     */
    private static int partSuffixLength(final long totalParts) {
        return Math.max(4, String.valueOf(totalParts).length());
    }

    /**
     * Finishes one {@code splitByLine} part: closes the writer - which flushes it first, preserving the flush
     * failure and suppressing any close failure - and returns it to the pool either way.
     *
     * @param partWriter the writer for the part that has just been filled.
     * @throws IOException if the part could not be flushed or closed.
     */
    private static void closeSplitPart(final BufferedWriter partWriter) throws IOException {
        try {
            partWriter.close();
        } finally {
            Objectory.recycle(partWriter);
        }
    }

    /**
     * Splits a file into at most {@code numOfParts} parts <b>on line boundaries</b>, writing the parts into the
     * source file's own directory, reading and writing as UTF-8.
     *
     * <p>This is the line-oriented counterpart of {@link #split(File, int, File)}, which cuts at exact byte
     * offsets and can therefore split a line - or a multi-byte character - in half. Use this one when each part
     * has to stay independently readable.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File log = new File("app.log");
     * IOUtil.splitByLine(log, 4);
     * // Creates app_0001.log .. app_0004.log next to app.log
     * }</pre>
     *
     * @param file       the source file to split; must exist and be readable.
     * @param numOfParts the maximum number of parts to split the file into; must be greater than 0.
     * @throws IllegalArgumentException if {@code file} is {@code null} or {@code numOfParts} is less than 1.
     * @throws FileNotFoundException if the source file does not exist or is not readable.
     * @throws IOException if reading lines from {@code file} or creating or writing the part files fails.
     * @see #splitByLine(File, int, File, Charset)
     * @see #split(File, int, File)
     * @see #merge(Collection, File)
     */
    public static void splitByLine(final File file, final int numOfParts) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(file, cs.file);

        splitByLine(file, numOfParts, file.getAbsoluteFile().getParentFile(), DEFAULT_CHARSET);
    }

    /**
     * Splits a file into at most {@code numOfParts} parts <b>on line boundaries</b>, writing the parts into
     * {@code destDir}, reading and writing as UTF-8.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOUtil.splitByLine(new File("app.log"), 4, new File("chunks"));
     * // Creates chunks/app_0001.log .. chunks/app_0004.log
     * }</pre>
     *
     * @param file       the source file to split; must exist and be readable.
     * @param numOfParts the maximum number of parts to split the file into; must be greater than 0.
     * @param destDir    the directory where the split parts will be stored; it is created if it does not exist and must be writable.
     * @throws IllegalArgumentException if {@code file} is {@code null}, {@code numOfParts} is less than 1, or
     *         {@code destDir} is {@code null} or is an existing file.
     * @throws FileNotFoundException if the source file does not exist or is not readable.
     * @throws IOException if the destination directory cannot be created or written to, or another I/O error occurs.
     * @see #splitByLine(File, int, File, Charset)
     * @see #split(File, int, File)
     */
    public static void splitByLine(final File file, final int numOfParts, final File destDir) throws IllegalArgumentException, IOException {
        splitByLine(file, numOfParts, destDir, DEFAULT_CHARSET);
    }

    /**
     * Splits a file into at most {@code numOfParts} parts <b>on line boundaries</b>, using the specified charset
     * both to decode the source and to encode the parts.
     *
     * <p>File naming convention:</p>
     * <ul>
     *   <li><strong>Pattern:</strong> {@code baseName_NNNN.extension} - the number is inserted before the
     *       extension, unlike {@link #split(File, int, File)}, which appends it to the whole file name. A
     *       {@code .gz}/{@code .zip} suffix is dropped first, since the parts hold decompressed text:
     *       {@code app.log.gz} yields {@code app_0001.log}. A name with no extension, and a dot-file whose
     *       leading dot is its name rather than an extension, take the number at the end instead:
     *       {@code logfile} yields {@code logfile_0001} and {@code .hidden} yields {@code .hidden_0001}.</li>
     *   <li><strong>Numbering:</strong> Sequential zero-padded numbers starting from 1, at least 4 digits wide and
     *       widened to the digit count of {@code numOfParts}, so the part names always sort in part order - which
     *       {@link #merge(Collection, File)} relies on.</li>
     *   <li><strong>Example:</strong> {@code app.log} becomes {@code app_0001.log}, {@code app_0002.log}, etc.</li>
     *   <li><strong>Existing parts:</strong> a file already present under a generated part name is truncated and overwritten.</li>
     * </ul>
     *
     * <p>Size distribution:</p>
     * <ul>
     *   <li>Lines are never split, so parts are equal in <i>line count</i>, not in bytes.</li>
     *   <li>The lines-per-part figure comes from a line-count estimate that <b>samples</b> a plain or
     *       {@code .zip} source - so those parts are only approximately equal - but counts a {@code .gz} source
     *       <b>exactly</b>, because a compressed stream cannot be sampled meaningfully.</li>
     *   <li>Where the figure is an estimate and it under-counts, the surplus lines all go into the last part
     *       rather than spilling into extra files: <b>at most {@code numOfParts} parts are ever written</b>. If it
     *       over-counts, fewer parts are written.</li>
     *   <li>An empty source file produces no parts at all.</li>
     * </ul>
     *
     * <p>Unlike {@link #split(File, int, File)} and {@link #splitBySize(File, long, File)}, which read the file
     * literally, a {@code .gz} or {@code .zip} source <b>is</b> decompressed - the parts are text, so splitting
     * the raw bytes of an archive would only produce mangled binary. A {@code .zip} yields the lines of its
     * <i>first non-directory entry</i> alone, exactly as the {@code read*} family reads one. The parts are not
     * byte-preserving in any case: every one is terminated with {@code '\n'} regardless of the line separators
     * in the source, so merging the parts does not reproduce the original byte for byte.
     *
     * <p>Part sizes come from a sampled line-count estimate, except for a {@code .gz} source, which is counted
     * exactly: a compressed stream cannot be sampled meaningfully, because the decompressor pulls in almost the
     * whole file before the sample has yielded its lines.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOUtil.splitByLine(new File("app.log"), 4, new File("chunks"), StandardCharsets.ISO_8859_1);
     * }</pre>
     *
     * @param file       the source file to split; must exist and be readable.
     * @param numOfParts the maximum number of parts to split the file into; must be greater than 0.
     * @param destDir    the directory where the split parts will be stored; it is created if it does not exist and must be writable.
     * @param charset    the charset used to decode the source and encode the parts; if {@code null}, the default
     *                   charset (UTF-8) is used.
     * @throws IllegalArgumentException if {@code file} is {@code null}, if {@code numOfParts} is less than 1, or
     *         if {@code destDir} is {@code null} or is an existing file, or a part path aliases the source file.
     * @throws FileNotFoundException if the source file does not exist or is not readable.
     * @throws IOException if the destination directory cannot be created or written to, or another I/O error occurs.
     * @see #splitByLine(File, int, File)
     * @see #split(File, int, File)
     * @see #merge(Collection, File)
     */
    public static void splitByLine(final File file, final int numOfParts, final File destDir, final Charset charset)
            throws IllegalArgumentException, IOException {
        // Validate everything before createDestDirectory, which is the first step that touches the filesystem,
        // so a rejected call leaves no new directory behind (matching split/splitBySize).
        N.checkArgNotNull(file, cs.file);
        N.checkArgPositive(numOfParts, cs.numOfParts);
        checkFileExists(file);
        createDestDirectory(destDir);

        // A null charset means UTF-8 here as it does in every other charset-taking method in this class. This
        // one used to be the single exception, rejecting null with IllegalArgumentException, so a caller
        // forwarding a nullable charset failed on exactly one method of the family.
        final Charset partCharset = checkCharset(charset);

        // partSuffixLength, not String.valueOf(numOfParts).length(): a bare digit count gives width 1 for
        // numOfParts = 9, and then _10 would sort between _1 and _2 in a directory listing that merge(..) walks
        // in order. The roll-over below is capped at numOfParts so this width is always enough.
        final int suffixLen = partSuffixLength(numOfParts);

        final long estimatedLineCount = estimateLineCount(file, 10000, partCharset);
        // Ceiling division (like the byte-based split sibling): flooring would create more parts than requested.
        final long lineNumOfPart = N.max((estimatedLineCount + numOfParts - 1) / numOfParts, 1);

        // The parts hold decompressed TEXT, so a ".gz"/".zip" suffix on the source must not be carried over to
        // them: "app.log.gz" has to yield "app_0001.log", not a plain-text file called "app.log_0001.gz" that
        // this class's own read* family would then try to gunzip.
        String baseName = file.getName();
        final String lowerCaseName = baseName.toLowerCase(Locale.ROOT);

        if (lowerCaseName.endsWith(GZ) || lowerCaseName.endsWith(ZIP)) {
            baseName = baseName.substring(0, baseName.length() - (lowerCaseName.endsWith(GZ) ? GZ.length() : ZIP.length()));
        }

        // index > 0, not >= 0: the leading dot of a dot-file (".hidden") is part of its NAME, not the start
        // of an extension. Treating it as one left prefix empty, so ".hidden" split into "_0001.hidden" -
        // a part name carrying nothing of the source. It now yields ".hidden_0001", the shape split(..) uses.
        final int index = baseName.lastIndexOf('.');
        final String prefix = index > 0 ? baseName.substring(0, index) : baseName;
        final String postfix = index > 0 ? baseName.substring(index) : "";

        // Opened like the read* family: a ".gz"/".zip" source is decompressed, because this method produces TEXT
        // parts. Splitting the raw bytes of an archive by line would emit mangled binary, and unlike
        // split/splitBySize the parts are not byte-preserving anyway (line separators are normalized below), so
        // there is nothing to be gained by reading literally.
        withOpenedFile(file, is -> {
            // Decode with the caller's charset. Neither the decoder nor the pooled BufferedReader is closed
            // here: Objectory.recycle(..) does not close the underlying source, and withOpenedFile(..) already
            // closes the stream the whole chain sits on.
            final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(is, partCharset));
            BufferedWriter bw = null;
            int serNum = 1;
            long lineCounter = 0;

            try {
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    if (bw == null) {
                        final String subFileName = destDir.getAbsolutePath() + IOUtil.DIR_SEPARATOR + prefix + "_"
                                + Strings.padStart(N.stringOf(serNum++), suffixLen, '0') + postfix;
                        final File part = new File(subFileName);
                        requireCanonicalPathsNotEquals(file, part);
                        bw = Objectory.createBufferedWriter(openFileWriter(part, partCharset));
                    }

                    bw.write(line);
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    lineCounter++;

                    // Roll over to a new part only while one is still owed. The line count that sized the parts
                    // is an ESTIMATE, so without this cap an under-estimate would keep opening files past
                    // numOfParts - overflowing the name width computed above and breaking the sort order.
                    // Surplus lines go into the final part instead.
                    if ((lineCounter % lineNumOfPart) == 0 && serNum <= numOfParts) {
                        final BufferedWriter finished = bw;
                        bw = null;
                        closeSplitPart(finished);
                    }
                }

                if (bw != null) {
                    // Cleared BEFORE closing, which is what makes the cleanup below unambiguous: on the success
                    // path bw is null by the time finally runs, so a non-null bw there means the loop threw.
                    // Closing the last part is part of writing it - a failure flushing it is the operation's
                    // real failure and must propagate as the checked IOException this method declares.
                    final BufferedWriter last = bw;
                    bw = null;
                    closeSplitPart(last);
                }
            } finally {
                if (bw != null) {
                    // Reached only when something above threw. Close QUIETLY: a secondary failure releasing a
                    // half-written part must not replace the real cause, and must not escape a method whose
                    // signature promises IOException as the unchecked exception IOUtil.close(..) would raise -
                    // both of which is what calling close(bw) from here used to do.
                    closeQuietly(bw);
                    Objectory.recycle(bw);
                }

                Objectory.recycle(br);
            }

            return (Void) null;
        });
    }

    /**
     * What {@link #estimateLineCount(File, int, Charset)} needs in order to scale a sample of the head of a
     * source up to the whole of it: a counter, and the total the counter's bytes are measured against.
     *
     * <p>{@link #openFile(File, Holder, SampleScale)} decides where the counter goes, because only it knows how
     * the stream was built - under the decompressor for {@code .gz} (compressed bytes vs. the file length), on the
     * entry stream for {@code .zip} (decompressed bytes vs. the entry's declared size), on the file itself
     * otherwise. Either way the caller divides the same two numbers.
     */
    private static final class SampleScale {
        private CountingInputStream counter;

        /** Bytes the counter is measured against, or a non-positive value when the source cannot be scaled. */
        private long total = -1;
    }

    /**
     * Wraps {@code in} in a counter and records it in {@code sampleScale}, or returns {@code in} unchanged when no
     * sampling was requested.
     */
    private static InputStream scaleBy(final InputStream in, final SampleScale sampleScale, final long total) {
        if (sampleScale == null) {
            return in;
        }

        sampleScale.counter = new CountingInputStream(in);
        sampleScale.total = total;

        return sampleScale.counter;
    }

    /**
     * Counts the bytes read from the stream it wraps.
     *
     * <p>Used to measure how much of a source a line sample consumed. That is strictly better than re-encoding
     * each sampled line and adding one byte for the separator: it counts the real terminators (a CRLF is two
     * bytes, not one), counts any byte-order mark, and costs nothing per line.
     */
    private static final class CountingInputStream extends InputStream {
        private final InputStream in;

        private long count;

        CountingInputStream(final InputStream in) {
            this.in = in;
        }

        long count() {
            return count;
        }

        @Override
        public int read() throws IOException {
            final int b = in.read();

            if (b != EOF) {
                count++;
            }

            return b;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            final int n = in.read(b, off, len);

            if (n != EOF) {
                count += n;
            }

            return n;
        }

        @Override
        public long skip(final long n) throws IOException {
            final long skipped = in.skip(n);
            count += skipped;

            return skipped;
        }

        @Override
        public int available() throws IOException {
            return in.available();
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    /**
     * Counts the lines of a file, exactly where that is the only way to be right and by sampling where sampling
     * works. The source is opened the way the {@code read*} family opens it, so a {@code .gz}/{@code .zip} name
     * <b>is</b> decompressed - matching what {@link #splitByLine(File, int, File, Charset)} then reads.
     *
     * <p>Three cases, and the reason they differ:
     * <ul>
     *   <li><b>Plain file</b> - sampled. A {@link CountingInputStream} on the file measures what the sample
     *       consumed, and that divides into {@link File#length()}: same unit, and the sample is a small fraction
     *       of the whole.</li>
     *   <li><b>{@code .zip}</b> - sampled. The counter sits on the entry stream and measures <i>decompressed</i>
     *       bytes, which divide into the entry's declared {@link java.util.zip.ZipEntry#getSize()}. Same unit
     *       again. An entry of unknown size simply disables the scaling.</li>
     *   <li><b>{@code .gz}</b> - <b>counted exactly</b>, by reading the whole source. Sampling cannot work here:
     *       the counter has to sit <i>under</i> the decompressor to share a unit with {@link File#length()}, but
     *       by the time the sample has produced its lines the decompressor has already pulled in essentially the
     *       whole compressed stream, so the ratio collapses to "the sample is the file" and the result sticks at
     *       {@code byReadingLineNum}. Measured before this was fixed: 100,000 lines in a 6.8&nbsp;KB gzip came
     *       back as 10,000, splitting four ways as 2500/2500/2500/92500.</li>
     * </ul>
     *
     * <p>Measuring the stream also beats re-encoding each line and adding one byte for the separator: real line
     * terminators count for what they are (a CRLF is two bytes) and a byte-order mark is included. On the sampled
     * paths the buffered reader may pull up to one buffer past the last line it handed out, which slightly
     * over-states the sample and so under-states the result - a bounded error against a sample of
     * {@code byReadingLineNum} lines, erring toward fewer, larger parts.
     *
     * @param file the file whose line count is to be estimated, must not be {@code null}.
     * @param byReadingLineNum the number of lines to sample; must be positive. For a {@code .gz} source this is
     *        validated but does not limit reading, because that source is counted in full.
     * @param charset the charset the file is encoded in, used to decode the lines.
     * @return the line count - exact for a {@code .gz} source or a source shorter than the sample, otherwise an
     *         estimate. Never negative, and never below the number of lines actually read.
     * @throws IllegalArgumentException if {@code byReadingLineNum} is not positive.
     * @throws IOException if opening {@code file} or reading its sampled lines fails. Deliberately checked: the public
     *         {@link #splitByLine(File, int, File, Charset)} declares {@code IOException}, so wrapping here would
     *         leak an {@link UncheckedIOException} out of a method whose signature promises the checked form.
     */
    private static long estimateLineCount(final File file, final int byReadingLineNum, final Charset charset) throws IllegalArgumentException, IOException {
        N.checkArgPositive(byReadingLineNum, cs.byReadingLineNum);

        if (file.length() == 0) {
            return 0;
        }

        final boolean exact = file.getName().toLowerCase(Locale.ROOT).endsWith(GZ);

        final SampleScale scale = new SampleScale();
        final Holder<ZipFile> zipHolder = new Holder<>();

        // No counter on the exact path: its result is never read there, and the wrapper would add an indirection
        // to every read of a full-file pass.
        final InputStream opened = openFile(file, zipHolder, exact ? null : scale);

        try (final ZipFile zipFile = zipHolder.value(); // NOSONAR - closed here, not used in the body
             final InputStream in = opened) {
            // Decode with the caller's charset: sampling as UTF-8 would mis-split lines for any other encoding.
            final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(in, charset));

            try {
                long cnt = 0;

                while ((exact || cnt < byReadingLineNum) && br.readLine() != null) {
                    cnt++;
                }

                if (exact || cnt < byReadingLineNum) {
                    // Either the whole source fit in the sample, or it was counted in full: exact, not an estimate.
                    return cnt;
                }

                final long consumed = scale.counter == null ? 0 : scale.counter.count();

                if (scale.total <= 0 || consumed <= 0) {
                    // Nothing to scale against (an unknown ZIP entry size, say). The sample is all we know.
                    return cnt;
                }

                final long averageLineLength = Math.max(1, consumed / cnt); // cnt > 0: it reached byReadingLineNum

                // Never report fewer lines than were actually read.
                return Math.max(cnt, scale.total / averageLineLength);
            } finally {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * Merges the given source files into the specified destination file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File[] sourceFiles = {new File("part1.txt"), new File("part2.txt")};
     * File destination = new File("merged.txt");
     * long bytesWritten = IOUtil.merge(sourceFiles, destination);
     *
     * // IOUtil.merge((File[]) null, destination);   // throws IllegalArgumentException; destination is untouched
     * }</pre>
     *
     * @param sourceFiles an array of files to be merged, must not be {@code null}. Each must be an existing,
     *                    readable file. An empty array still <i>replaces</i> the destination with nothing - see
     *                    {@link #merge(Collection, File)}.
     * @param destFile    the destination file where the merged content will be written. It is created if it does not exist, and overwritten if it does.
     * @return the number of bytes written to the destination file.
     * @throws IllegalArgumentException if {@code sourceFiles} or {@code destFile} is {@code null}, if {@code sourceFiles} holds a
     *         {@code null} element, if any source file is a directory, or if any source file denotes the same file as
     *         {@code destFile}.
     * @throws FileNotFoundException if any source file does not exist or is not readable.
     * @throws IOException if reading a file in {@code sourceFiles} or opening or writing {@code destFile} fails.
     * @see #split(File, int, File)
     * @see #splitBySize(File, long, File)
     */
    public static long merge(final File[] sourceFiles, final File destFile) throws IllegalArgumentException, IOException {
        // Array.asList(null) yields an EMPTY list, which would silently turn a null argument into
        // "merge zero files" - and merging zero files truncates destFile. Reject it as a bad argument
        // before anything opens the destination, so a null can never destroy an existing file.
        N.checkArgNotNull(sourceFiles, cs.sourceFiles);

        return merge(Array.asList(sourceFiles), destFile);
    }

    /**
     * Merges the given source files into the specified destination file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume a.txt contains "foo" and b.txt contains "bar".
     * File a = new File("a.txt");
     * File b = new File("b.txt");
     * File merged = new File("merged.txt");
     * long n = IOUtil.merge(Arrays.asList(a, b), merged);             // returns 6; merged holds "foobar"
     * long m = IOUtil.merge(Collections.<File>emptyList(), merged);   // returns 0; merged is now EMPTY
     * }</pre>
     *
     * <p><b>An empty source collection still replaces the destination.</b> Like every {@code write}-family
     * method targeting a {@code File}, {@code merge} truncates {@code destFile} before writing, so merging zero
     * files leaves it existing and empty rather than untouched. A {@code null} collection is rejected as a bad
     * argument instead, so a forgotten null check cannot silently erase the destination.
     *
     * @param sourceFiles a collection of files to be merged, must not be {@code null}. Each must be an existing,
     *                    readable file.
     * @param destFile    the destination file where the merged content will be written. It is created if it does not exist, and overwritten if it does.
     * @return the number of bytes written to the destination file.
     * @throws IllegalArgumentException if {@code sourceFiles} or {@code destFile} is {@code null}, if {@code sourceFiles} holds a
     *         {@code null} element, if any source file is a directory, or if any source file denotes the same file as
     *         {@code destFile}.
     * @throws FileNotFoundException if any source file does not exist or is not readable.
     * @throws IOException if reading a file in {@code sourceFiles} or opening or writing {@code destFile} fails.
     * @see #split(File, int, File)
     * @see #splitBySize(File, long, File)
     */
    public static long merge(final Collection<File> sourceFiles, final File destFile) throws IllegalArgumentException, IOException {
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
     *
     * // Merge with delimiter (e.g., newline between files)
     * long bytesWritten = IOUtil.merge(parts, "\n".getBytes(StandardCharsets.UTF_8), merged);
     *
     * // An empty delimiter concatenates the parts back-to-back
     * IOUtil.merge(parts, N.EMPTY_BYTE_ARRAY, merged);
     * }</pre>
     *
     * <p><b>An empty source collection still replaces the destination:</b> {@code destFile} is truncated before
     * writing, so merging zero files leaves it existing and empty. A {@code null} collection is rejected as a bad
     * argument instead.
     *
     * @param sourceFiles a collection of files to be merged, must not be {@code null}. Each must be an existing,
     *                    readable file.
     * @param delimiter   a byte array that will be inserted between each file during the merge; {@code null} or empty inserts nothing.
     * @param destFile    the destination file where the merged content will be written. It is created if it does not exist, and overwritten if it does.
     * @return the number of bytes written to the destination file, including the delimiters.
     * @throws IllegalArgumentException if {@code sourceFiles} or {@code destFile} is {@code null}, if {@code sourceFiles} holds a
     *         {@code null} element, if any source file is a directory, or if any source file denotes the same file as
     *         {@code destFile}.
     * @throws FileNotFoundException if any source file does not exist or is not readable.
     * @throws IOException if reading a file in {@code sourceFiles} or opening or writing {@code destFile} fails.
     * @see #split(File, int, File)
     * @see #splitBySize(File, long, File)
     */
    public static long merge(final Collection<File> sourceFiles, final byte[] delimiter, final File destFile) throws IllegalArgumentException, IOException {
        N.checkArgNotNull(destFile, cs.destFile);

        // A null collection is a programming error, not "merge nothing": without this check the loop below
        // would throw NullPointerException instead of the documented IllegalArgumentException.
        N.checkArgNotNull(sourceFiles, cs.sourceFiles);

        final byte[] buf = Objectory.createByteArrayBuffer();

        long totalCount = 0;

        try {
            // Validate all sources BEFORE opening the destination: openFileOutputStream creates/truncates
            // the destination file, which would destroy an existing destination when a source is invalid.
            // A source that IS the destination is also rejected here - it would be truncated before being
            // read (silent data loss), or read back the freshly merged bytes instead of its old content.
            for (final File file : sourceFiles) {
                checkFileExists(file);

                requireCanonicalPathsNotEquals(file, destFile);
            }

            try (OutputStream output = openFileOutputStream(destFile)) {
                int idx = 0;

                for (final File file : sourceFiles) {
                    if (idx++ > 0 && N.notEmpty(delimiter)) {
                        output.write(delimiter);
                        totalCount += delimiter.length;
                    }

                    try (InputStream input = openFileInputStream(file)) {
                        int count = 0;
                        while (EOF != (count = read(input, buf, 0, buf.length))) {
                            if (count == 0) {
                                break;
                            }

                            output.write(buf, 0, count);

                            totalCount += count;
                        }
                    }
                }

                output.flush();
            }
        } finally {
            Objectory.recycle(buf);
        }

        return totalCount;
    }

    /**
     * Lists the immediate entries of the specified parent directory, subdirectories included.
     * Subdirectories are not descended into.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("data");
     * List<File> files = IOUtil.listFiles(directory);
     * for (File file : files) {
     *     System.out.println(file.getName());
     * }
     * }</pre>
     *
     * @param parentPath the parent directory from which to list entries. If it is {@code null} or does not exist, an empty list is
     *                   returned; if it exists but is not a directory, that is a bad argument and is rejected.
     * @return a new, modifiable list of the direct children (files and subdirectories) of the parent directory.
     * @throws IllegalArgumentException if {@code parentPath} exists but is not a directory.
     * @see #listFiles(File, boolean, boolean)
     * @see #listDirectories(File)
     * @see Stream#listFiles(File)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static List<File> listFiles(final File parentPath) throws IllegalArgumentException {
        return listFiles(parentPath, false, false);
    }

    /**
     * Lists all files in the specified parent directory.
     * If the {@code recursively} parameter is set to {@code true}, it will list files in all subdirectories as well.
     * If the {@code excludeDirectory} parameter is set to {@code true}, it will exclude directories from the list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("data");
     * List<File> files = IOUtil.listFiles(directory, true, true);
     * for (File file : files) {
     *     System.out.println(file.getName());
     * }
     * }</pre>
     *
     * <p><b>Order:</b> depth-first, pre-order. Within one directory, entries come in {@link File#listFiles()}
     * order, which is filesystem-dependent; a directory is followed immediately by everything beneath it, before
     * its own next sibling. {@link #walk(File, boolean, boolean)} produces exactly the same sequence lazily.
     * When {@code excludeDirectory} is {@code true}, directories are left out of the result but are still
     * descended into.
     *
     * @param parentPath       the parent directory from which to list entries. If it is {@code null} or does not exist, an empty list is
     *                         returned; if it exists but is not a directory, that is a bad argument and is rejected.
     * @param recursively      a boolean indicating whether to list files in all subdirectories. Symbolic links to directories are never descended into.
     * @param excludeDirectory a boolean indicating whether to exclude directories from the list.
     * @return a new, modifiable list of the matching entries under the parent directory.
     * @throws IllegalArgumentException if {@code parentPath} exists but is not a directory.
     * @see #listFiles(File, boolean, Throwables.BiPredicate)
     * @see Stream#listFiles(File, boolean, boolean)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static List<File> listFiles(final File parentPath, final boolean recursively, final boolean excludeDirectory) throws IllegalArgumentException {
        return listFiles(parentPath, recursively, excludeDirectory ? directories_excluded_filter : all_files_filter);
    }

    /**
     * Lists all files in the specified parent directory.
     * If the {@code recursively} parameter is set to {@code true}, it will list files in all subdirectories as well.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File directory = new File("data");
     * List<File> files = IOUtil.listFiles(directory, true,
     *     (parent, file) -> file.getName().endsWith(".txt"));
     * for (File file : files) {
     *     System.out.println(file.getName());
     * }
     * }</pre>
     *
     * @param <E>         the type of the exception that may be thrown by the filter.
     * @param parentPath  the parent directory where the listing will start. If it is {@code null} or does not exist, an empty list is returned.
     * @param recursively if {@code true}, files in all subdirectories of the parent directory will be listed. Symbolic links to directories are never descended into.
     * @param filter      a BiPredicate that takes the parent directory and a file as arguments and returns a boolean. If the predicate returns {@code true}, the file is listed; if it returns {@code false}, the file is not listed. A rejected subdirectory is still descended into when {@code recursively} is {@code true}.
     * @return a new, modifiable list of the matching files in the specified directory and possibly its subdirectories.
     * @throws IllegalArgumentException if {@code filter} is {@code null}, or if {@code parentPath} exists but is not a directory.
     * @throws E if the filter throws an exception.
     * @see Stream#listFiles(File, boolean, boolean)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static <E extends Exception> List<File> listFiles(final File parentPath, final boolean recursively,
            final Throwables.BiPredicate<? super File, ? super File, E> filter) throws IllegalArgumentException, E {
        N.checkArgNotNull(filter, cs.filter);

        final List<File> files = new ArrayList<>();

        if (parentPath == null || !parentPath.exists()) {
            return files;
        }

        // A path that exists but is not a directory is a wrong-KIND argument, which the class contract reports
        // as IllegalArgumentException. It used to come back as an empty list - indistinguishable from an empty
        // directory - so passing the wrong File read as "there is nothing here". A null or missing parentPath
        // stays an empty result: those are the documented "nothing to list" answers callers probe with.
        checkListableDirectory(parentPath);

        listFiles0(parentPath, recursively, filter, files);

        return files;
    }

    /**
     * The recursion behind {@link #listFiles(File, boolean, Throwables.BiPredicate)}, for a directory the caller
     * has already validated. Recursing through the public method instead re-ran {@code exists()} and
     * {@code isDirectory()} on every subdirectory the walk had just classified as one.
     *
     * <p>Order is depth-first, pre-order: an entry is appended, then everything beneath it, then its next sibling.
     * {@link DepthFirstFileIterator} reproduces this sequence lazily for {@code walk(..)}.
     *
     * @param dir         a directory that exists and is not a symbolic link.
     * @param recursively whether to descend into subdirectories.
     * @param filter      selects what is appended; a rejected subdirectory is still descended into.
     * @param files       the result being built.
     * @throws E if the filter throws.
     */
    private static <E extends Exception> void listFiles0(final File dir, final boolean recursively,
            final Throwables.BiPredicate<? super File, ? super File, E> filter, final List<File> files) throws E {
        final File[] subFiles = dir.listFiles();

        if (N.isEmpty(subFiles)) {
            return;
        }

        for (final File file : subFiles) {
            if (filter.test(dir, file)) {
                files.add(file);
            }

            // Avoid infinite recursion on cyclic symbolic links: don't descend into symlinked directories.
            if (recursively && file.isDirectory() && !isSymbolicLinkOrJunction(file)) {
                listFiles0(file, recursively, filter, files);
            }
        }
    }

    /**
     * Rejects a path the listing family was handed that exists but is not a directory.
     *
     * <p>Only the wrong-<i>kind</i> half of the class's path rule is enforced here: a {@code null} or
     * non-existent {@code parentPath} is the documented "nothing to list" answer and still yields an empty
     * result, because callers legitimately probe with one.
     *
     * @param parentPath the path being listed; already known to be non-{@code null} and to exist.
     * @throws IllegalArgumentException if it is not a directory.
     */
    private static void checkListableDirectory(final File parentPath) throws IllegalArgumentException {
        if (!parentPath.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(parentPath) + "' is not a directory");
        }
    }

    /**
     * Lists all directories in the specified parent directory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File parentDir = new File("parent");
     * List<File> subdirs = IOUtil.listDirectories(parentDir);
     * }</pre>
     *
     * @param parentPath the parent directory from which to list directories. If it is {@code null} or does not exist, an empty list is
     *                   returned; if it exists but is not a directory, that is a bad argument and is rejected.
     * @return a new, modifiable list of the direct subdirectories of the parent directory.
     * @throws IllegalArgumentException if {@code parentPath} exists but is not a directory.
     * @see #listDirectories(File, boolean)
     * @see Stream#listFiles(File)
     * @see Fn#isDirectory()
     */
    public static List<File> listDirectories(final File parentPath) throws IllegalArgumentException {
        return listDirectories(parentPath, false);
    }

    /**
     * Lists all directories in the specified parent directory.
     * If the {@code recursively} parameter is set to {@code true}, it will list directories in all subdirectories as well.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File parentDir = new File("parent");
     * List<File> subdirs = IOUtil.listDirectories(parentDir, true);
     * }</pre>
     *
     * <p><b>Order:</b> the same depth-first, pre-order traversal as
     * {@link #listFiles(File, boolean, boolean)} - a directory is followed immediately by the directories
     * beneath it, before its own next sibling - so this returns exactly the directory entries of
     * {@code listFiles(parentPath, recursively, false)}, in the same relative order.
     *
     * @param parentPath  the parent directory from which to list directories. If it is {@code null} or does not exist, an empty list is
     *                    returned; if it exists but is not a directory, that is a bad argument and is rejected.
     * @param recursively a boolean indicating whether to list directories in all subdirectories. Symbolic links to directories are never descended into.
     * @return a new, modifiable list of File objects representing all directories in the parent directory and its subdirectories if recursively is {@code true}.
     * @throws IllegalArgumentException if {@code parentPath} exists but is not a directory.
     * @see Stream#listFiles(File, boolean, boolean)
     * @see Fn#isDirectory()
     */
    public static List<File> listDirectories(final File parentPath, final boolean recursively) throws IllegalArgumentException {
        return listFiles(parentPath, recursively, directories_only_filter);
    }

    /**
     * Returns a {@link Stream} of all files and directories in the specified parent directory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File rootDir = new File("root");
     * Stream<File> fileStream = IOUtil.walk(rootDir);
     * fileStream.forEach(f -> System.out.println(f.getPath()));
     * }</pre>
     *
     * <p>This is the lazy counterpart of {@link #listFiles(File)} and answers the same inputs the same way:
     * a {@code null} or non-existent {@code parentPath} yields an empty stream rather than an exception.
     *
     * @param parentPath the parent directory from which to list files and directories. If it is {@code null} or does not exist, an empty
     *                   stream is returned; if it exists but is not a directory, that is a bad argument and is rejected.
     * @return a {@link Stream} of {@link File} objects representing all files and directories in the parent directory
     *         (its direct children only; subdirectories are not descended into).
     * @throws IllegalArgumentException if {@code parentPath} exists but is not a directory.
     * @see #listFiles(File)
     * @see Stream#listFiles(File)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static Stream<File> walk(final File parentPath) throws IllegalArgumentException {
        return walk(parentPath, false, false);
    }

    /**
     * Returns a {@link Stream} of files and directories in the specified parent directory.
     * If the {@code recursively} parameter is set to {@code true}, it will include files in all subdirectories as well.
     * If the {@code excludeDirectory} parameter is set to {@code true}, it will exclude directories from the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File rootDir = new File("root");
     * Stream<File> fileStream = IOUtil.walk(rootDir, true, true);
     * fileStream.forEach(f -> System.out.println(f.getPath()));
     * }</pre>
     *
     * <p>This is the lazy counterpart of {@link #listFiles(File, boolean, boolean)}: same traversal <i>and the
     * same order</i>, same treatment of a {@code null} or non-existent {@code parentPath} (an empty stream), same
     * symbolic-link policy. It differs only in producing elements on demand instead of materializing a
     * {@code List}, so {@code IOUtil.walk(d, r, e).toList()} equals {@code IOUtil.listFiles(d, r, e)} element for
     * element. Nothing is read from the filesystem at {@code walk(..)} time beyond the argument check: the
     * top-level listing, like every deeper one, is taken when the stream is first advanced, so an entry created
     * between building the stream and consuming it is seen. Note that
     * {@link Stream#listFiles(File, boolean, boolean)} - which this method no longer delegates to - walks the same
     * tree <b>breadth-first</b> instead.
     *
     * @param parentPath       the parent directory from which to list files and directories. If it is {@code null} or does not exist, an empty
     *                         stream is returned; if it exists but is not a directory, that is a bad argument and is rejected.
     * @param recursively      a boolean indicating whether to list files in all subdirectories. As in
     *                         {@link #listFiles(File, boolean, boolean)}, symbolic links to directories are never
     *                         descended into, so a cyclic link cannot make the traversal loop.
     * @param excludeDirectory a boolean indicating whether to exclude directories from the list.
     * @return a {@link Stream} of {@link File} objects representing the files and directories in the parent directory.
     * @throws IllegalArgumentException if {@code parentPath} exists but is not a directory.
     * @see #listFiles(File, boolean, boolean)
     * @see Stream#listFiles(File, boolean, boolean)
     * @see Fn#isFile()
     * @see Fn#isDirectory()
     */
    public static Stream<File> walk(final File parentPath, final boolean recursively, final boolean excludeDirectory) throws IllegalArgumentException {
        // Stream.listFiles(..) rejects a null parentPath, but the eager twin listFiles(..) answers null and a
        // missing directory with an empty result. The two are documented as the same operation, so they must
        // not disagree on the same input; the guard belongs here rather than in Stream, whose own contract
        // legitimately requires a non-null argument.
        if (parentPath == null || !parentPath.exists()) {
            return Stream.empty();
        }

        // Same wrong-kind rejection as the eager twin: the two are documented as the same traversal, so they
        // must not disagree on the same input.
        checkListableDirectory(parentPath);

        // Deliberately NOT Stream.listFiles(..), which walks breadth-first: the eager twin listFiles(..)
        // recurses, so it is depth-first pre-order, and the two therefore returned the same entries in
        // different orders while this method's own javadoc promised "same traversal ... differs only in
        // producing elements on demand". The divergence survived because the test pinning the twins together
        // compared them after sorting both sides. Fixed here rather than in Stream.listFiles(..), which is
        // public in its own right and has other callers that may rely on its breadth-first order.
        return Stream.of(new DepthFirstFileIterator(parentPath, recursively, excludeDirectory));
    }

    /**
     * The lazy form of {@link IOUtil#listFiles(File, boolean, Throwables.BiPredicate)}'s recursion: depth-first,
     * pre-order, one directory listing held per level rather than the whole tree.
     *
     * <p>Order is the point. The eager twin emits an entry and then, immediately, everything beneath it before
     * moving to the entry's next sibling; reproducing that lazily needs an explicit stack of directory listings,
     * because the children of an entry have to be produced before the siblings already sitting in the parent's
     * listing. A queue would give breadth-first order instead, which is what {@code Stream.listFiles(..)} does.
     *
     * <p>An excluded entry is still descended into - the flag selects what is <i>emitted</i>, not what is
     * traversed - which is exactly what the eager twin does with its filter.
     */
    private static final class DepthFirstFileIterator extends ObjIterator<File> {

        /** One directory listing, plus how far into it the traversal has got. */
        private static final class Level {
            private final File[] files;
            private int cursor;

            Level(final File[] files) {
                this.files = files;
            }
        }

        /** Deepest level last. One entry per directory whose listing is only partly consumed. */
        private final Deque<Level> stack = new ArrayDeque<>();

        private final boolean recursively;

        private final boolean excludeDirectory;

        /**
         * The directory to start from, held until the first {@link #advance()} and then dropped. Listing it in the
         * constructor made {@code walk(..)} read the top level at call time - so a stream built before an entry
         * was created never saw it - while every deeper level was listed on demand. Now nothing is read until the
         * first {@code hasNext()}/{@code next()}, which is what "lazy" promises.
         */
        private File root;

        private File next;

        /**
         * Whether {@link #next} has been computed. Exhaustion is tracked with {@link #exhausted} rather than by
         * reading a {@code null} {@code next} as "no more", so the iterator protocol never depends on what value
         * an element happens to have. ({@code File.listFiles()} never yields a {@code null} element; if it did,
         * {@code advance()} would fail on {@code isDirectory()} rather than end the traversal quietly.)
         */
        private boolean nextReady;

        private boolean exhausted;

        DepthFirstFileIterator(final File parentPath, final boolean recursively, final boolean excludeDirectory) {
            this.recursively = recursively;
            this.excludeDirectory = excludeDirectory;
            this.root = parentPath;
        }

        @Override
        public boolean hasNext() {
            if (!nextReady) {
                advance();
                nextReady = true;
            }

            return !exhausted;
        }

        @Override
        public File next() {
            if (!hasNext()) {
                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
            }

            final File result = next;
            next = null;
            nextReady = false;

            return result;
        }

        /** Computes {@link #next}, or sets {@link #exhausted} when the traversal is over. */
        private void advance() {
            if (root != null) {
                push(root.listFiles());
                root = null;
            }

            while (true) {
                final Level level = topLevelWithEntries();

                if (level == null) {
                    exhausted = true;

                    return;
                }

                final File file = level.files[level.cursor++];
                final boolean isDirectory = file.isDirectory();

                // Pushed BEFORE the entry is handed back, so the next call reads from this new level: that is
                // what makes the order pre-order rather than "all siblings, then all of their children".
                // A symlinked directory is emitted but never descended into - isDirectory() follows the link,
                // so descending would walk out of the tree and loop forever on a cyclic one. Same exclusion as
                // the eager twin.
                if (recursively && isDirectory && !isSymbolicLinkOrJunction(file)) {
                    push(file.listFiles());
                }

                if (!excludeDirectory || !isDirectory) {
                    next = file;

                    return;
                }

                // Excluded from the result, but its children (already pushed) are not.
            }
        }

        /** Drops exhausted levels and returns the deepest one that still has an entry, or {@code null}. */
        @MayReturnNull
        private Level topLevelWithEntries() {
            while (!stack.isEmpty()) {
                final Level level = stack.peekLast();

                if (level.cursor < level.files.length) {
                    return level;
                }

                stack.removeLast();
            }

            return null;
        }

        /** {@code listFiles()} answers {@code null} for an I/O error, which reads here as "nothing below". */
        private void push(final File[] files) {
            if (N.notEmpty(files)) {
                stack.addLast(new Level(files));
            }
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Converts from a {@code URL} to a {@code File}.
     * <p>
     * This method will decode the URL.
     * Syntax such as {@code file:///my%20docs/file.txt} will be
     * correctly decoded to {@code /my docs/file.txt}. This method uses UTF-8 to decode
     * percent-encoded octets to characters.
     * Additionally, malformed percent-encoded octets are handled leniently by
     * passing them through literally.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = IOUtil.toFile(new URL("file:///my%20docs/file.txt"));   // path contains "my docs/file.txt" (decoded)
     * IOUtil.toFile(new URL("file://server/share/f.txt"));                // UNC: \\server\share\f.txt
     * // IOUtil.toFile(new URL("http://example.com/x.txt"));             // throws IllegalArgumentException (not a file URL)
     * // IOUtil.toFile(null);                                            // throws IllegalArgumentException
     * }</pre>
     *
     * <p>A query string or fragment is not part of a file location, so anything from the first {@code ?} or
     * {@code #} onwards is dropped rather than becoming part of the file name.
     *
     * <p>A percent-encoded path separator ({@code %2F} or {@code %5C}, in either case) is <b>rejected</b>.
     * Escaping a separator asks for it to be part of a file <i>name</i>, and decoding it into a live separator
     * instead is how {@code file:///tmp/a%2F..%2Fb} used to come back as {@code /tmp/a/../b} - an encoded
     * traversal segment, the usual way of hiding one from a naive check, surviving into the result. Both
     * characters are rejected on every platform, for the reason {@link #unzip(File, File)} treats both as
     * separators everywhere: a path that is a harmless name on one host is a traversal on the other, and a URL
     * is just as likely to have been built on the other one.
     *
     * <p>A non-empty authority is treated as a UNC host and kept: {@code file://server/share/f.txt} becomes
     * {@code \\server\share\f.txt}, so {@link #toUrl(File)} round-trips a UNC path. An empty authority
     * ({@code file:///c:/x}) and {@code localhost} both mean the local machine and yield a plain local path.
     * The one exception is a Windows drive letter, which the URL parser reports as the authority of the
     * malformed-but-common two-slash form: {@code file://C:/x} means the drive {@code C:}, not a host called
     * {@code C:}, and yields {@code C:\x} rather than {@code \\C:\x}. An authority carrying a port or
     * user info is rejected, and so is an IPv6 literal host ({@code file://[::1]/share}): a file URL addresses
     * a filesystem, and splicing any of those into a UNC name yields a path the platform cannot even parse.
     *
     * @param url the file URL to convert, must not be {@code null}.
     * @return a File object corresponding to the input URL.
     * @throws IllegalArgumentException if {@code url} is {@code null}, the URL is not a file URL, its
     *         authority carries a port, user info, or an IPv6 literal host, or its path percent-encodes a
     *         path separator.
     */
    public static File toFile(final URL url) throws IllegalArgumentException {
        N.checkArgNotNull(url, cs.url);

        if (!url.getProtocol().equals("file")) {
            throw new IllegalArgumentException("URL could not be converted to a File: " + url);
        }

        // URL.getFile() is path + query, and the ref/fragment can be smuggled in by hand-built URLs. Neither
        // names a file, so cut them off before they end up inside the file name (e.g. "/a.txt?v=1").
        String path = url.getPath();
        final int refIndex = path.indexOf('#');

        if (refIndex >= 0) {
            path = path.substring(0, refIndex);
        }

        // A non-empty authority is a UNC host: "file://server/share/x.txt" means \\server\share\x.txt, and
        // getPath() drops the "server". Dropping it silently produced a valid-looking but WRONG local path
        // ("\share\x.txt"), which also broke the toUrl/toFile round trip for UNC paths. "localhost" and the
        // empty authority both mean "this machine", so only those keep the plain local form.
        final String authority = url.getAuthority();

        if (Strings.isNotEmpty(authority) && !"localhost".equalsIgnoreCase(authority)) {
            if (isWindowsDriveAuthority(authority)) {
                path = "/" + authority + path;
            } else {
                // A file URL addresses a filesystem, so it has nowhere to put a port or credentials. Splicing
                // them into a UNC name produces a path the platform then rejects - "\\host:80\share\f.txt" fails
                // Path conversion with InvalidPathException: Illegal character [:] - so reject them here, where
                // the argument is still identifiable, rather than handing back something unusable.
                if (url.getPort() != -1 || url.getUserInfo() != null) {
                    throw new IllegalArgumentException("A file URL cannot carry a port or user info in its authority: " + authority + " (from " + url + ")");
                }

                // An IPv6 literal - "file://[::1]/share" - fails for the same reason: its colons are not legal
                // in a path, so "\\[::1]\share" is rejected by Path conversion. Windows does have a UNC
                // spelling for IPv6 (the ipv6-literal.net form), but translating to it is a separate feature;
                // until then, refusing beats returning something unusable. The same check rejects an empty
                // host, which would otherwise build a host-less "\\\\share".
                final String host = url.getHost();

                if (Strings.isEmpty(host) || host.indexOf(':') >= 0) {
                    throw new IllegalArgumentException("A file URL needs a plain host name in its authority, not '" + authority + "' (from " + url + ")");
                }

                path = "//" + host + path;
            }
        }

        // Checked on the still-encoded form, and decoded BEFORE the separator substitution. Decoding afterwards
        // let "%2F" become a live path separator: "file:///tmp/a%2F..%2Fb" came back as "/tmp/a/../b", so an
        // encoded traversal segment - the usual way of hiding one from a naive check - survived into the
        // returned File. A separator that was percent-encoded was deliberately escaped by whoever built the URL
        // and is therefore part of a NAME, which no filesystem here can express.
        if (containsEscapedSeparator(path)) {
            throw new IllegalArgumentException("A file URL cannot contain a percent-encoded path separator: " + url);
        }

        return new File(decodeUrl(path).replace('/', File.separatorChar));
    }

    /**
     * Returns the numeric value of a single ASCII hex digit, or {@code -1} if the character is not one.
     *
     * <p>Deliberately not {@link Character#digit(char, int)}: that accepts non-ASCII digits (Arabic-Indic
     * and friends), which are not legal in a percent-encoded octet.
     *
     * @param c the character to decode.
     * @return the value 0..15, or {@code -1} if {@code c} is not an ASCII hex digit.
     */
    private static int hexDigit(final char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }

        return -1;
    }

    /**
     * Whether {@code path} percent-encodes a path separator ({@code %2F} or {@code %5C}, in either case).
     *
     * <p>Checked on the still-encoded form: once decoded, an escaped separator is indistinguishable from a
     * literal one, and treating it as literal is exactly the confusion this rejects. Both characters count on
     * every platform - {@code '\'} is a legal name character on Unix, but a URL carrying one is far more
     * likely to have come from a Windows producer than to mean a file whose name really contains it.
     *
     * <p>{@code %2F} and {@code %5C} are the only escapes that can yield a separator: UTF-8 encodes both
     * characters in a single byte, and the decoder rejects overlong forms such as {@code %C0%AF} rather than
     * folding them back to {@code '/'}.
     *
     * @param path the raw, still percent-encoded URL path.
     * @return {@code true} if it encodes a separator.
     */
    private static boolean containsEscapedSeparator(final String path) {
        // The last index at which a complete three-character escape can start: charAt(i + 2) is read below, so
        // a path ending in a truncated "%2" or a bare "%" must not enter the body at all.
        for (int i = 0, to = path.length() - 3; i <= to; i++) {
            if (path.charAt(i) == '%') {
                final char c1 = path.charAt(i + 1);
                final char c2 = path.charAt(i + 2);

                if ((c1 == '2' && (c2 == 'F' || c2 == 'f')) || (c1 == '5' && (c2 == 'C' || c2 == 'c'))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Whether {@code authority} is a Windows drive letter such as {@code "C:"} rather than a host name.
     *
     * <p>A drive letter can only reach the authority position through a malformed two-slash file URL
     * ({@code file://C:/x} instead of {@code file:///C:/x}), which several tools emit; a real host name can never
     * take this form, since {@code ':'} in an authority introduces a port.
     *
     * @param authority the URL authority to classify.
     * @return {@code true} if it is a single ASCII letter followed by a colon.
     */
    private static boolean isWindowsDriveAuthority(final String authority) {
        return authority.length() == 2 && authority.charAt(1) == ':' && isDriveLetter(authority.charAt(0));
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
     * @return the decoded URL or {@code null} if the input was {@code null}.
     */
    private static String decodeUrl(final String url) {
        String decoded = url;
        if (url != null && url.indexOf('%') >= 0) {
            final int n = url.length();
            final StringBuilder buffer = new StringBuilder();
            final ByteBuffer bytes = ByteBuffer.allocate(n);
            for (int i = 0; i < n;) {
                if (url.charAt(i) == '%') {
                    final int startOfPercent = i;
                    try {
                        do {
                            // Integer.parseInt(.., 16) accepts a sign, so "%-1" used to decode to the byte
                            // 0xFF (rendered as U+FFFD) instead of being reported as malformed and kept
                            // literally the way a truncated "%2" or a bare "%" already were.
                            final int high = hexDigit(url.charAt(i + 1));
                            final int low = hexDigit(url.charAt(i + 2));

                            if (high < 0 || low < 0) {
                                throw new NumberFormatException("Not a percent-encoded octet: " + url.substring(i, i + 3));
                            }

                            bytes.put((byte) ((high << 4) + low));
                            i += 3;
                        } while (i < n && url.charAt(i) == '%');
                    } catch (final RuntimeException e) {
                        // malformed percent-encoded octet, append the '%' literally and advance past it
                        if (i == startOfPercent) {
                            buffer.append('%');
                            i++;
                        }
                    } finally {
                        if (bytes.position() > 0) {
                            bytes.flip();
                            buffer.append(DEFAULT_CHARSET.decode(bytes));
                            bytes.clear();
                        }
                    }
                } else {
                    buffer.append(url.charAt(i++));
                }
            }
            decoded = buffer.toString();
        }
        return decoded;
    }

    /**
     * Converts each {@code URL} in the specified array to a {@code File}.
     * <p>
     * Returns an array of the same size as the input.
     * If the input is empty, an empty array is returned; a {@code null} array is rejected, matching
     * {@link #toFiles(Collection)}, {@link #toUrls(File[])} and {@link #toUrls(Collection)}.
     * <p>
     * This method will decode the URL.
     * Syntax such as {@code file:///my%20docs/file.txt} will be
     * correctly decoded to {@code /my docs/file.txt}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url1 = new File("file1.txt").toURI().toURL();
     * URL url2 = new File("file2.txt").toURI().toURL();
     * File[] files = IOUtil.toFiles(new URL[] { url1, url2 });   // converts URLs to Files
     * File[] empty = IOUtil.toFiles(new URL[0]);                 // returns empty array
     * }</pre>
     *
     * @param urls the file URLs to convert, must not be {@code null}; an empty array returns an empty array.
     * @return a non-{@code null} array of Files matching the input.
     * @throws IllegalArgumentException if {@code urls} is {@code null}, or if any URL is {@code null}, is not a
     *         file URL, or otherwise cannot be converted by {@link #toFile(URL)} (for example, because its
     *         authority contains a port, user info, or an IPv6 literal host).
     * @see #toFile(URL)
     */
    public static File[] toFiles(final URL[] urls) throws IllegalArgumentException {
        N.checkArgNotNull(urls, cs.urls);

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL url1 = new File("file1.txt").toURI().toURL();
     * URL url2 = new File("file2.txt").toURI().toURL();
     * List<File> files = IOUtil.toFiles(Arrays.asList(url1, url2));      // converts to list of Files
     * List<File> empty = IOUtil.toFiles(Collections.<URL>emptyList());   // returns empty list
     * }</pre>
     *
     * @param urls the collection of URLs to be converted, must not be {@code null}.
     * @return a list of File objects corresponding to the input URLs; an empty list if {@code urls} is empty.
     * @throws IllegalArgumentException if {@code urls} is {@code null}, or if any URL in the collection is
     *         {@code null}, is not a file URL, or otherwise cannot be converted by {@link #toFile(URL)} (for
     *         example, because its authority contains a port, user info, or an IPv6 literal host).
     * @see #toFile(URL)
     */
    public static List<File> toFiles(final Collection<URL> urls) throws IllegalArgumentException {
        N.checkArgNotNull(urls, cs.urls);

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.txt");
     * URL url = IOUtil.toUrl(file);
     * }</pre>
     *
     * @param file the File object to be converted, must not be {@code null}.
     * @return a URL object corresponding to the input File object.
     * @throws IllegalArgumentException if {@code file} is {@code null}.
     * @throws UncheckedIOException if converting {@code file} to a URL produces an invalid URL.
     * @see #toFile(URL)
     * @see File#toURI()
     * @see URI#toURL()
     */
    public static URL toUrl(final File file) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(file, cs.file);

        try {
            return file.toURI().toURL();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Converts an array of File objects into an array of corresponding URL objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File[] files = {new File("file1.txt"), new File("file2.txt")};
     * URL[] urls = IOUtil.toUrls(files);
     * }</pre>
     *
     * @param files the array of File objects to be converted, must not be {@code null} and must not contain {@code null}.
     * @return an array of URL objects corresponding to the input File objects; an empty array if {@code files} is empty.
     * @throws IllegalArgumentException if {@code files} is {@code null} or contains a {@code null} element.
     * @throws UncheckedIOException if converting a file in {@code files} to a URL produces an invalid URL.
     * @see #toUrl(File)
     * @see File#toURI()
     * @see URI#toURL()
     */
    public static URL[] toUrls(final File[] files) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(files, cs.files);

        if (N.isEmpty(files)) {
            return new URL[0];
        }

        final URL[] urls = new URL[files.length];

        for (int i = 0; i < urls.length; i++) {
            urls[i] = toUrl(files[i]);
        }

        return urls;
    }

    /**
     * Converts a collection of File objects into a list of corresponding URL objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<File> files = Arrays.asList(new File("a.txt"), new File("b.txt"));
     * List<URL> urls = IOUtil.toUrls(files);
     * }</pre>
     *
     * @param files the collection of File objects to be converted, must not be {@code null} and must not contain {@code null}.
     * @return a list of URL objects corresponding to the input File objects; an empty list if {@code files} is empty.
     * @throws IllegalArgumentException if {@code files} is {@code null} or contains a {@code null} element.
     * @throws UncheckedIOException if converting a file in {@code files} to a URL produces an invalid URL.
     * @see #toUrl(File)
     * @see File#toURI()
     * @see URI#toURL()
     */
    public static List<URL> toUrls(final Collection<File> files) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(files, cs.files);

        if (N.isEmpty(files)) {
            return new ArrayList<>();
        }

        final List<URL> urls = new ArrayList<>(files.size());

        for (final File file : files) {
            urls.add(toUrl(file));
        }

        return urls;
    }

    /**
     * Creates the specified file if it does not exist, and otherwise sets its last-modified time to the current
     * system time - the behaviour of the Unix {@code touch} command.
     *
     * <p>Missing parent directories are created as needed. Use {@link #updateLastModified(File)} instead when an
     * absent file must <i>not</i> be created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("marker.txt");
     * IOUtil.touch(file);   // creates marker.txt if absent, otherwise bumps its timestamp
     * }</pre>
     *
     * @param source the File to create or whose last-modified timestamp is to be updated, must not be {@code null}.
     *        A dangling symbolic link is an existing entry to {@code File.createNewFile()}; this method creates the
     *        link's target through it, as {@code touch(1)} does. A dangling Windows junction has a directory for a
     *        target, which cannot be created as a file: that is reported as a failure to create the file.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException if the file could not be created, or its timestamp could not be updated.
     * @see #updateLastModified(File)
     * @see #createFileIfNotExists(File)
     */
    public static void touch(final File source) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, cs.source);

        try {
            if (createNewFileIfNotExists(source)) {
                // A freshly created file already carries the current time.
                return;
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to create file: " + describe(source), e);
        }

        if (source.setLastModified(System.currentTimeMillis())) {
            return;
        }

        // The entry exists but its target does not: a dangling link, which createNewFile() counts as existing and
        // setLastModified() cannot stamp. touch(1) creates the link's target through the link; so does this.
        if (!source.exists() && existsOrIsDanglingLink(source)) {
            try {
                openFileOutputStream(source, true).close();
                return;
            } catch (final IOException e) {
                throw new UncheckedIOException("Failed to create file: " + describe(source), e);
            }
        }

        throw new UncheckedIOException(new IOException("Failed to update the last-modified time of: " + describe(source)));
    }

    /**
     * Sets the last-modified time of an existing file or directory to the current system time. Unlike
     * {@link #touch(File)} this never creates anything: if {@code source} is {@code null} or does not exist,
     * {@code false} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("marker.txt");
     * boolean updated = IOUtil.updateLastModified(file);   // false if marker.txt does not exist
     * }</pre>
     *
     * @param source the File whose last-modified timestamp is to be updated. Can be {@code null}.
     * @return {@code true} if the file exists and its last-modified time was updated successfully;
     *         {@code false} if {@code source} is {@code null}, does not exist, or could not be updated.
     * @see #touch(File)
     */
    public static boolean updateLastModified(final File source) {
        if (source == null) {
            return false;
        }

        return source.exists() && source.setLastModified(System.currentTimeMillis());
    }

    /**
     * Tests whether the contents of two files are equal.
     * <p>
     * This method checks to see if the two files are different lengths or if they point to the same file, before
     * resorting to byte-by-byte comparison of the contents.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file1 = new File("file1.txt");
     * File file2 = new File("file2.txt");
     * boolean areEqual = IOUtil.contentEquals(file1, file2);
     * }</pre>
     *
     * <p>If both arguments are the same reference or both are {@code null}, returns {@code true}; if only one
     * is {@code null}, returns {@code false} - unless one of the arguments exists and is a <i>directory</i>,
     * which is rejected before those short circuits, so {@code contentEquals(dir, dir)} and
     * {@code contentEquals(null, dir)} both throw {@link IllegalArgumentException}. Any other wrong kind (a FIFO,
     * a socket, a device node) is only recognised once the comparison reaches the files themselves and is
     * reported as an {@link UncheckedIOException}, so it is still hidden by the short circuits above.
     * Note that a bare {@code contentEquals(null, null)} does not
     * compile - the {@code File}, {@code InputStream} and {@code Reader} overloads are all applicable - so cast
     * the arguments when that is what you mean.
     *
     * @param file1 the first file. May be {@code null}.
     * @param file2 the second file. May be {@code null}.
     * @return {@code true} if the contents of the files are equal, they both don't exist, or both are
     *         {@code null}; {@code false} otherwise.
     * @throws IllegalArgumentException if an input is not a file.
     * @throws UncheckedIOException if opening or reading {@code file1} or {@code file2} while comparing their contents fails
     * @see #contentEqualsIgnoreEOL(File, File, String)
     */
    public static boolean contentEquals(final File file1, final File file2) throws IllegalArgumentException, UncheckedIOException {
        try {
            return contentEquals0(file1, file2);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean contentEquals0(final File file1, final File file2) throws IOException {
        // A directory is not a file: reject it up-front (even when both arguments are the same directory
        // reference), consistent with the documented "@throws IllegalArgumentException if an input is not a file".
        if (file1 != null && file1.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(file1) + "' is not a file");
        } else if (file2 != null && file2.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(file2) + "' is not a file");
        }

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

        checkFileExists(file1, cs.file1);
        checkFileExists(file2, cs.file2);

        if (file1.length() != file2.length()) {
            // lengths differ, cannot be equal
            return false;
        }

        if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
            // same file
            return true;
        }

        try (final InputStream input1 = openFileInputStream(file1); //
             final InputStream input2 = openFileInputStream(file2)) {
            return contentEquals(input1, input2);
        }
    }

    /**
     * Compares the contents of two files to determine if they are equal, ignoring end-of-line differences.
     * <p>
     * This method checks to see if the two files point to the same file,
     * before resorting to line-by-line comparison of the contents.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Assume unix.txt contains "line1\nline2", windows.txt contains the same text with CRLF,
     * // and different.txt contains "line1\nDIFFERENT".
     * File a = new File("unix.txt");
     * File b = new File("windows.txt");
     * File c = new File("different.txt");
     * boolean x = IOUtil.contentEqualsIgnoreEOL(a, b, "UTF-8");                                   // returns true (EOL differences ignored)
     * boolean y = IOUtil.contentEqualsIgnoreEOL(a, c, "UTF-8");                                   // returns false
     * boolean z = IOUtil.contentEqualsIgnoreEOL(a, b, null);                                      // returns true (null charsetName uses default)
     * // two non-existing files compare equal:
     * boolean w = IOUtil.contentEqualsIgnoreEOL(new File("nope1"), new File("nope2"), "UTF-8");   // returns true
     * }</pre>
     *
     * <p>If both arguments are the same reference or both are {@code null}, returns {@code true}; if only one
     * is {@code null}, returns {@code false} - unless one of the arguments exists and is a <i>directory</i>,
     * which is rejected before those short circuits, so {@code contentEqualsIgnoreEOL(dir, dir, charsetName)}
     * and {@code contentEqualsIgnoreEOL(null, dir, charsetName)} both throw {@link IllegalArgumentException}.
     * Any other wrong kind (a FIFO, a socket, a device node) is only recognised once the comparison reaches the
     * files themselves and is reported as an {@link UncheckedIOException}, so it is still hidden by the short
     * circuits above.
     *
     * @param file1       the first file. May be {@code null}.
     * @param file2       the second file. May be {@code null}.
     * @param charsetName the name of the requested charset.
     *                    May be {@code null} or empty, in which case the default charset (UTF-8) is used.
     * @return {@code true} if the content of the files are equal or neither exists,
     *         {@code false} otherwise. The comparison is line-based, so a trailing line terminator is not
     *         significant: a file holding {@code "a\n"} equals one holding {@code "a"}.
     * @throws IllegalArgumentException if an input is not a file.
     * @throws IllegalCharsetNameException if {@code charsetName} is not a legal charset name
     *         (unchecked exception).
     * @throws UnsupportedCharsetException if the named charset is not available in this JVM (unchecked exception).
     *         The name is resolved before the comparison short-circuits, so it is rejected even when the two
     *         arguments are the same file, canonicalize to the same file, or both do not exist. A <i>directory</i>
     *         argument still outranks it and is reported first - unlike {@link #readAllToString(File, String)} and
     *         {@link #readAllLines(File, String)}, which resolve the charset before looking at the file at all and
     *         so report the charset even for a directory.
     * @throws UncheckedIOException in case of an I/O error.
     * @see IOUtil#contentEqualsIgnoreEOL(Reader, Reader)
     */
    public static boolean contentEqualsIgnoreEOL(final File file1, final File file2, final String charsetName)
            throws IllegalArgumentException, IllegalCharsetNameException, UnsupportedCharsetException, UncheckedIOException {
        try {
            return contentEqualsIgnoreEOL0(file1, file2, charsetName);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean contentEqualsIgnoreEOL0(final File file1, final File file2, final String charsetName) throws IOException {
        // A directory is not a file: reject it up-front (even when both arguments are the same directory
        // reference), consistent with the documented "@throws IllegalArgumentException if an input is not a file".
        if (file1 != null && file1.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(file1) + "' is not a file");
        } else if (file2 != null && file2.isDirectory()) {
            throw new IllegalArgumentException("'" + describe(file2) + "' is not a file");
        }

        // Resolved before the short circuits below, not after them: a charset name that names no charset is a
        // bad argument whatever the files turn out to be. Left until it was needed, the same bogus name was
        // rejected for two distinct files but silently accepted whenever an early return fired first - the same
        // object twice, two paths that canonicalize to one file, or two files that both do not exist. It stays
        // *after* the wrong-kind check above, so a directory argument still outranks it.
        final Charset charset = checkCharset(charsetName);

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

        checkFileExists(file1, cs.file1);
        checkFileExists(file2, cs.file2);

        if (file1.getCanonicalFile().equals(file2.getCanonicalFile())) {
            // same file
            return true;
        }

        // Opened the way contentEquals0(..) opens its files, so the two twins report a failed open identically.
        try (Reader input1 = new InputStreamReader(openFileInputStream(file1), charset);
             Reader input2 = new InputStreamReader(openFileInputStream(file2), charset)) {
            return contentEqualsIgnoreEOL(input1, input2);
        }
    }

    /**
     * Compares the contents of two InputStreams to determine if they are equal or not.
     * <p>
     * If both inputs are the same reference or both are {@code null}, returns {@code true}.
     * If only one is {@code null}, returns {@code false}.
     * </p>
     *
     * <p><b>Stream position:</b> both streams are read to end of input, or to the first difference; either way
     * the position afterwards is unspecified and neither is meant to be continued from.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is1 = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8));
     *      InputStream is2 = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8))) {
     *     boolean equal = IOUtil.contentEquals(is1, is2);   // returns true
     * }
     * try (InputStream is1 = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8));
     *      InputStream is2 = new ByteArrayInputStream("xyz".getBytes(StandardCharsets.UTF_8))) {
     *     boolean equal = IOUtil.contentEquals(is1, is2);   // returns false
     * }
     * }</pre>
     *
     * @param input1 the first stream.
     * @param input2 the second stream.
     * @return {@code true} if the content of the streams are equal (or both are {@code null}), {@code false} otherwise.
     * @throws UncheckedIOException if reading {@code input1} or {@code input2} while comparing their contents fails
     */
    public static boolean contentEquals(final InputStream input1, final InputStream input2) throws UncheckedIOException {
        // Before making any changes, please test with
        // org.apache.commons.io.jmh.IOUtilsContentEqualsInputStreamsBenchmark
        if (input1 == input2) {
            return true;
        }

        if (input1 == null || input2 == null) {
            return false;
        }

        // Acquired inside the try that returns them: taking the second buffer before entering it would
        // strand the first in a failure between the two.
        byte[] buffer1 = null;
        byte[] buffer2 = null;

        try {
            buffer1 = Objectory.createByteArrayBuffer();
            buffer2 = Objectory.createByteArrayBuffer();

            final int bufferSize = buffer1.length;
            int pos1 = 0, pos2 = 0, count1 = 0, count2 = 0;

            while (true) {
                pos1 = 0;
                pos2 = 0;

                for (int index = 0; index < bufferSize; index++) {
                    if (pos1 == index) {
                        count1 = readWithProgress(input1, buffer1, pos1, bufferSize - pos1);

                        if (count1 == EOF) {
                            return pos2 == index && input2.read() == EOF;
                        }

                        pos1 += count1;
                    }

                    if (pos2 == index) {
                        count2 = readWithProgress(input2, buffer2, pos2, bufferSize - pos2);

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
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buffer1);
            Objectory.recycle(buffer2);
        }
    }

    private static int readWithProgress(final InputStream input, final byte[] buffer, final int offset, final int length) throws IOException {
        final int count = input.read(buffer, offset, length);

        if (count != 0) {
            return count;
        }

        // A non-zero-length blocking read should not return zero, but custom/channel-backed
        // streams occasionally do. A one-byte read guarantees progress or EOF without spinning.
        final int value = input.read();

        if (value == EOF) {
            return EOF;
        }

        buffer[offset] = (byte) value;
        return 1;
    }

    /**
     * Compares the contents of two Readers to determine if they are equal or not.
     * <p>
     * This method buffers the input internally, so there is no need to use a {@code BufferedReader}.
     * If both inputs are the same reference or both are {@code null}, returns {@code true}.
     * If only one is {@code null}, returns {@code false}.
     * </p>
     *
     * <p><b>Reader position:</b> both readers are read to end of input, or to the first difference; either way
     * the position afterwards is unspecified and neither is meant to be continued from.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader r1 = new StringReader("abc");
     *      Reader r2 = new StringReader("abc")) {
     *     boolean equal = IOUtil.contentEquals(r1, r2);   // returns true
     * }
     * try (Reader r1 = new StringReader("abc");
     *      Reader r2 = new StringReader("xyz")) {
     *     boolean equal = IOUtil.contentEquals(r1, r2);   // returns false
     * }
     * }</pre>
     *
     * @param input1 the first reader.
     * @param input2 the second reader.
     * @return {@code true} if the content of the readers are equal (or both are {@code null}), {@code false} otherwise.
     * @throws UncheckedIOException if reading {@code input1} or {@code input2} while comparing their contents fails
     */
    public static boolean contentEquals(final Reader input1, final Reader input2) throws UncheckedIOException {
        if (input1 == input2) {
            return true;
        }

        if (input1 == null || input2 == null) {
            return false;
        }

        // See contentEquals(InputStream, InputStream): acquired inside the try that returns them.
        char[] buffer1 = null;
        char[] buffer2 = null;

        try {
            buffer1 = Objectory.createCharArrayBuffer();
            buffer2 = Objectory.createCharArrayBuffer();

            final int bufferSize = buffer1.length;
            int pos1 = 0, pos2 = 0, count1 = 0, count2 = 0;

            while (true) {
                pos1 = 0;
                pos2 = 0;

                for (int index = 0; index < bufferSize; index++) {
                    if (pos1 == index) {
                        count1 = readWithProgress(input1, buffer1, pos1, bufferSize - pos1);

                        if (count1 == EOF) {
                            return pos2 == index && input2.read() == EOF;
                        }

                        pos1 += count1;
                    }

                    if (pos2 == index) {
                        count2 = readWithProgress(input2, buffer2, pos2, bufferSize - pos2);

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
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buffer1);
            Objectory.recycle(buffer2);
        }
    }

    private static int readWithProgress(final Reader input, final char[] buffer, final int offset, final int length) throws IOException {
        final int count = input.read(buffer, offset, length);

        if (count != 0) {
            return count;
        }

        final int value = input.read();

        if (value == EOF) {
            return EOF;
        }

        buffer[offset] = (char) value;
        return 1;
    }

    /**
     * Compares the contents of two Readers to determine if they are equal or not, ignoring EOL characters.
     * <p>
     * This method buffers the input internally using
     * {@link BufferedReader} if they are not already buffered.
     * </p>
     *
     * <p><b>Reader position:</b> both readers are read to end of input, or to the first differing line; either
     * way the position afterwards is unspecified and neither is meant to be continued from.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader r1 = new StringReader("a\nb\nc");
     *      Reader r2 = new StringReader("a\r\nb\r\nc")) {
     *     boolean equal = IOUtil.contentEqualsIgnoreEOL(r1, r2);  // returns true (EOL differences ignored)
     * }
     * try (Reader r1 = new StringReader("abc");
     *      Reader r2 = new StringReader("xyz")) {
     *     boolean equal = IOUtil.contentEqualsIgnoreEOL(r1, r2);  // returns false (content differs)
     * }
     * }</pre>
     *
     * <p>Because the comparison is line-based, a trailing line terminator is not significant: {@code "a\n"} and
     * {@code "a"} compare equal, as do {@code "a\n"} and {@code "a\r\n"}. Two empty readers compare equal.
     *
     * @param input1 the first reader.
     * @param input2 the second reader.
     * @return {@code true} if the content of the readers are equal (ignoring EOL differences), {@code false} otherwise.
     * @throws UncheckedIOException if reading {@code input1} or {@code input2} while comparing their contents fails.
     */
    public static boolean contentEqualsIgnoreEOL(final Reader input1, final Reader input2) throws UncheckedIOException {
        if (input1 == input2) {
            return true;
        }

        if (input1 == null || input2 == null) {
            return false;
        }

        final boolean isInput1BufferedReader = IOUtil.isBufferedReader(input1);
        final boolean isInput2BufferedReader = IOUtil.isBufferedReader(input2);
        // See contentEquals(InputStream, InputStream): acquired inside the try that recycles them.
        BufferedReader br1 = null;
        BufferedReader br2 = null;

        try {
            br1 = isInput1BufferedReader ? (BufferedReader) input1 : Objectory.createBufferedReader(input1); //NOSONAR
            br2 = isInput2BufferedReader ? (BufferedReader) input2 : Objectory.createBufferedReader(input2); //NOSONAR

            String line1 = br1.readLine();
            String line2 = br2.readLine();

            while (line1 != null && line1.equals(line2)) {
                line1 = br1.readLine();
                line2 = br2.readLine();
            }

            return line1 == null && line2 == null;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
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
     * Parses the specified file/directory line by line.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.txt");
     * IOUtil.forEachLine(file, line -> System.out.println(line));
     * // Edge: empty file
     * IOUtil.forEachLine(new File("empty.txt"), line -> System.out.println(line));
     * }</pre>
     *
     * <p>This overload decodes the file as <b>UTF-8</b>. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E>        the type of exception that the lineAction can throw.
     * @param source     the source file or directory to process. If a directory, the regular files underneath it are read recursively (links to files included; a linked
     *        directory is not descended into, and special files and dangling links are left out - see the class contract).
     * @param lineAction a Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if opening {@code source} or reading lines from {@code source} fails
     * @throws E                    if the lineAction throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forEachLine(final File source, final Throwables.Consumer<? super String, E> lineAction)
            throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified file/directory line by line.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.txt");
     * IOUtil.forEachLine(file,
     *     line -> System.out.println(line),
     *     () -> System.out.println("Done"));
     * // Edge: empty file (no lines processed; onComplete still runs)
     * IOUtil.forEachLine(new File("empty.txt"),
     *     line -> System.out.println(line),
     *     () -> System.out.println("Done"));
     * }</pre>
     *
     * <p>This overload decodes the file as <b>UTF-8</b>. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param <E2> the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source the file or directory to process. If a directory, the regular files underneath it are read recursively (links to files included; a linked
     *        directory is not descended into, and special files and dangling links are left out - see the class contract).
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if opening {@code source} or reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final File source, final Throwables.Consumer<? super String, E> lineAction,
            final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        forEachLine(source, 0, Long.MAX_VALUE, lineAction, onComplete);
    }

    /**
     * Parses the specified file/directory line by line.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.txt");
     * IOUtil.forEachLine(file, 0, 100, line -> System.out.println(line));     // process first 100 lines
     * IOUtil.forEachLine(file, 10, 5, line -> System.out.println(line));
     * }</pre>
     *
     * <p>This overload decodes the file as <b>UTF-8</b>. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param source the file or directory to process. If a directory, the regular files underneath it are read recursively (links to files included; a linked
     *        directory is not descended into, and special files and dangling links are left out - see the class contract).
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, if the source is {@code null}, or if a
     *                                  {@code lineOffset} or {@code count} argument is negative.
     * @throws UncheckedIOException if opening {@code source} or reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forEachLine(final File source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction) throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, lineOffset, count, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified file/directory line by line.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.txt");
     * IOUtil.forEachLine(file, 0, 100,
     *     line -> System.out.println(line),
     *     () -> System.out.println("Done"));  // process first 100 lines
     * IOUtil.forEachLine(file, 10, 5,
     *     line -> System.out.println(line),
     *     () -> System.out.println("Done"));
     * }</pre>
     *
     * <p>This overload decodes the file as <b>UTF-8</b>. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param <E2> the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source the file or directory to process. If a directory, the regular files underneath it are read recursively (links to files included; a linked
     *        directory is not descended into, and special files and dangling links are left out - see the class contract).
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, if the source is
     *                                  {@code null}, or if a {@code lineOffset} or {@code count} argument is negative.
     * @throws UncheckedIOException if opening {@code source} or reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final File source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        doForEachLine(source, lineOffset, count, 0, 0, 0, DEFAULT_CHARSET, lineAction, onComplete);
    }

    /**
     * Parses the specified file/directory line by line, taking every slicing and concurrency setting from a named
     * {@link LineIterationOptions} builder instead of from positional numbers.
     *
     * <p>This is the only form that can request concurrency: the remaining positional overloads take at most a
     * {@code lineOffset}/{@code count} pair and always read and process on the calling thread. Each knob is named
     * on the builder rather than identified by its position in a run of numbers.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.txt");
     *
     * // skip 100 lines, then process the next 1000 on 4 worker threads
     * IOUtil.forEachLine(file,
     *     IOUtil.LineIterationOptions.builder().offset(100).count(1000).processThreads(4).build(),
     *     line -> process(line));
     * }</pre>
     *
     * <p>The {@code charset} of the supplied {@link LineIterationOptions} selects the encoding; it defaults
     * to UTF-8.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the file or directory to process. If a directory, the regular files underneath it are read recursively (links to files included; a linked
     *        directory is not descended into, and special files and dangling links are left out - see the class contract).
     * @param options    the slicing, concurrency and charset settings; {@code null} means "no slicing, read and
     *                   process on the calling thread, decode as UTF-8".
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if opening {@code source} or reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @see LineIterationOptions
     */
    public static <E extends Exception> void forEachLine(final File source, final LineIterationOptions options,
            final Throwables.Consumer<? super String, E> lineAction) throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, options, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified file/directory line by line, taking every slicing and concurrency setting from a named
     * {@link LineIterationOptions} builder instead of from positional numbers. See
     * {@link #forEachLine(File, LineIterationOptions, Throwables.Consumer)} for why this form is preferred.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOUtil.forEachLine(new File("data.txt"),
     *     IOUtil.LineIterationOptions.builder().count(1000).build(),
     *     line -> process(line),
     *     () -> System.out.println("Done"));
     * }</pre>
     *
     * <p>The {@code charset} of the supplied {@link LineIterationOptions} selects the encoding; it defaults
     * to UTF-8.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the file or directory to process. If a directory, the regular files underneath it are read recursively (links to files included; a linked
     *        directory is not descended into, and special files and dangling links are left out - see the class contract).
     * @param options    the slicing, concurrency and charset settings; {@code null} means "no slicing, read and
     *                   process on the calling thread, decode as UTF-8".
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if opening {@code source} or reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see LineIterationOptions
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final File source, final LineIterationOptions options,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        final LineIterationOptions opts = options == null ? DEFAULT_LINE_ITERATION_OPTIONS : options;

        doForEachLine(source, opts.offset(), opts.count(), opts.readThreads(), opts.processThreads(), opts.queueSize(), opts.charset(), lineAction, onComplete);
    }

    /**
     * Parses the given collection of files line by line using the provided lineAction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<File> files = Arrays.asList(new File("a.txt"), new File("b.txt"));
     * IOUtil.forEachLine(files, line -> System.out.println(line));
     * // Edge: empty file list
     * IOUtil.forEachLine(Collections.<File>emptyList(), line -> System.out.println(line));
     * }</pre>
     *
     * <p>This overload decodes the files as <b>UTF-8</b>. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E>        the type of exception that the lineAction can throw.
     * @param files      the collection of files/directories to process; no element may be {@code null}. A directory contributes the regular files
     *        underneath it, recursively (links to files included; a linked directory is not descended into, and special
     *        files and dangling links are left out - see the class contract).
     * @param lineAction a Consumer that takes a line of the file as a String and performs the desired operation.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if opening a file in {@code files} or reading its lines fails
     * @throws E                    if the lineAction throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forEachLine(final Collection<File> files, final Throwables.Consumer<? super String, E> lineAction)
            throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(files, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line using the provided lineAction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<File> files = Arrays.asList(new File("a.txt"), new File("b.txt"));
     * IOUtil.forEachLine(files,
     *     line -> System.out.println(line),
     *     () -> System.out.println("Done"));
     * // Edge: empty file list
     * IOUtil.forEachLine(Collections.<File>emptyList(),
     *     line -> System.out.println(line),
     *     () -> System.out.println("Done"));
     * }</pre>
     *
     * <p>This overload decodes the files as <b>UTF-8</b>. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E> the type of exception that the lineAction may throw during line processing.
     * @param <E2> the type of exception that the onComplete callback may throw after all lines are processed.
     * @param files the collection of files/directories to process; no element may be {@code null}. A directory contributes the regular files
     *        underneath it, recursively (links to files included; a linked directory is not descended into, and special
     *        files and dangling links are left out - see the class contract).
     * @param lineAction a Consumer that takes a line of the file as a String and performs the desired operation.
     * @param onComplete a Runnable that is executed after the parsing is complete.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if opening a file in {@code files} or reading its lines fails
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final Collection<File> files,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        forEachLine(files, 0, Long.MAX_VALUE, lineAction, onComplete);
    }

    /**
     * Parses the given collection of files line by line using the provided lineAction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<File> files = Arrays.asList(new File("a.txt"), new File("b.txt"));
     * IOUtil.forEachLine(files, 0, 100, line -> System.out.println(line));     // process first 100 lines
     * // Edge: empty file list
     * IOUtil.forEachLine(Collections.<File>emptyList(), 0, 100, line -> System.out.println(line));
     * }</pre>
     *
     * <p>This overload decodes the files as <b>UTF-8</b>. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param files      the collection of files/directories to process; no element may be {@code null}. A directory contributes the regular files
     *        underneath it, recursively (links to files included; a linked directory is not descended into, and special
     *        files and dangling links are left out - see the class contract).
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, if the source is {@code null}, or if a
     *                                  {@code lineOffset} or {@code count} argument is negative.
     * @throws UncheckedIOException if opening a file in {@code files} or reading its lines fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forEachLine(final Collection<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction) throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(files, lineOffset, count, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line using the provided lineAction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<File> files = Arrays.asList(new File("a.txt"), new File("b.txt"));
     * IOUtil.forEachLine(files, 0, 100,
     *     line -> System.out.println(line),
     *     () -> System.out.println("Done"));
     * // Edge: skip 10 lines, read 5 across multiple files
     * IOUtil.forEachLine(files, 10, 5,
     *     line -> System.out.println(line),
     *     () -> System.out.println("Done"));
     * }</pre>
     *
     * <p>This overload decodes the files as <b>UTF-8</b>. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param files      the collection of files/directories to process; no element may be {@code null}. A directory contributes the regular files
     *        underneath it, recursively (links to files included; a linked directory is not descended into, and special
     *        files and dangling links are left out - see the class contract).
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, if the source is
     *                                  {@code null}, or if a {@code lineOffset} or {@code count} argument is negative.
     * @throws UncheckedIOException if opening a file in {@code files} or reading its lines fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final Collection<File> files, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        doForEachLine(files, lineOffset, count, 0, 0, 0, DEFAULT_CHARSET, lineAction, onComplete);
    }

    /**
     * An {@link ObjIterator} over the lines of a single file that opens the file lazily (on the first
     * {@link #hasNext()} call) and closes the underlying reader as soon as the file is exhausted, or when
     * {@link #close()} is invoked. Opening lazily is what lets {@code forEachLine} process a directory holding a
     * very large number of files without holding one file descriptor per file open up front (which would
     * exhaust the process's file-descriptor limit before a single line had been read).
     *
     * <p>The file is opened with {@link #openFile(File, Holder)}, so a {@code .gz}/{@code .zip} name is
     * decompressed exactly as the {@code readAllLines}/{@code readLines} family decompresses it.
     */
    private static final class LazyFileLineIterator extends ObjIterator<String> implements AutoCloseable {
        private final File file;
        /** Held rather than resolved at construction: the file is not opened until the first hasNext(). */
        private final Charset charset;
        private ZipFile zipFile;
        private InputStream stream;
        private LineIterator lineIterator;
        /** Both volatile: {@link #close()} may run on the calling thread while a reader thread is in {@link #hasNext()}. */
        private volatile boolean opened = false;
        private volatile boolean closed = false;

        LazyFileLineIterator(final File file, final Charset charset) {
            this.file = file;
            this.charset = charset;
        }

        @Override
        public boolean hasNext() {
            // With readThreads > 0 the reader threads keep calling hasNext() while the calling thread, unwinding
            // from a failed line action or an early stop, closes every iterator. A reader that had passed this
            // check and was inside open() then finished opening a file that nobody would ever close - up to
            // readThreads files stayed open after every failed parallel forEachLine, until an unpredictable GC ran
            // the stream's cleaner (on Windows they could not be deleted meanwhile).
            // open() therefore publishes what it opened under the monitor close() takes, and releases it itself
            // when a close() has landed meanwhile; the open and the read both run outside the lock, so neither a
            // blocking open (a FIFO with no writer) nor a blocking read can pin the closing thread.
            if (closed) {
                return false;
            }

            if (!opened) {
                open();

                if (closed) {
                    return false;
                }
            }

            final boolean more;

            try {
                more = lineIterator.hasNext();
            } catch (final RuntimeException e) {
                // A close() that landed between the block above and the read makes the read fail on a closed
                // stream; for the reader that is "no more lines", not an error.
                if (closed) {
                    return false;
                }

                throw e;
            }

            if (more) {
                return !closed;
            }

            close(); // release the file descriptor as soon as this file is fully read
            return false;
        }

        /**
         * Opens the file through {@code openFile(..)} - the same entry point the {@code read*} family uses - so
         * that a {@code .gz}/{@code .zip} source yields its decompressed TEXT rather than its raw bytes decoded
         * as characters. Reading it literally used to hand the caller one "line" of replacement characters with
         * no error at all, on precisely the file kind {@code forEachLine} exists for: a rotated log too big to
         * read into memory.
         *
         * <p>Everything is opened into locals and published in one step under the monitor {@link #close()} takes.
         * A {@code close()} that ran while the file was being opened cannot have released it, so the opener
         * releases it here instead and leaves the iterator unopened; a failed open releases what it got and
         * rethrows, so it is retried-and-rethrown rather than leaving a null {@code lineIterator} behind.
         */
        private void open() {
            final Holder<ZipFile> zipHolder = new Holder<>();
            final InputStream openedStream;

            try {
                openedStream = openFile(file, zipHolder);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            final ZipFile openedZip = zipHolder.value();
            final LineIterator openedLines;

            try {
                openedLines = new LineIterator(newBufferedReader(openedStream, charset));
            } catch (final RuntimeException e) {
                closeQuietly(openedStream);
                closeQuietly(openedZip);

                throw e;
            }

            synchronized (this) {
                if (!closed && !opened) {
                    stream = openedStream;
                    zipFile = openedZip;
                    lineIterator = openedLines;
                    opened = true;

                    return;
                }
            }

            // Closed underneath us (or, defensively, opened twice): nobody else holds these handles, so this is
            // the only place they can be released.
            closeQuietly(openedLines);
            closeQuietly(openedStream);
            closeQuietly(openedZip);
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
            }

            return lineIterator.next();
        }

        @Override
        public synchronized void close() {
            release(true);
        }

        /**
         * Releases the file. The stream, and the {@code ZipFile} a ".zip" source sits on, are closed first: neither
         * close takes the reader's lock, so this never waits on a thread that is inside a read. The
         * {@code LineIterator} - whose {@code close()} takes that lock to close a reader already closed underneath
         * - is closed only when {@code closeReader}: the caller of {@code forEachLine} passes {@code false} when a
         * worker abandoned by {@code Iterators.forEach} after its bounded cancellation wait, or a reader thread that
         * a counted read left behind, may still be inside {@code readLine()}, where closing the reader pinned the
         * caller until the read returned - for ever on a FIFO or a device. That thread's next read fails on the
         * closed stream, which {@link #hasNext()} reports as "no more lines"; the reader itself holds nothing else.
         */
        synchronized void release(final boolean closeReader) {
            if (!closed) {
                closed = true;
                closeQuietly(stream);
                closeQuietly(zipFile);

                if (closeReader) {
                    closeQuietly(lineIterator);
                }
            }
        }
    }

    /**
     * Parses the given collection of files line by line, taking every slicing and concurrency setting from a named
     * {@link LineIterationOptions} builder instead of from positional numbers. See
     * {@link #forEachLine(File, LineIterationOptions, Throwables.Consumer)} for why this form is preferred.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<File> files = Arrays.asList(new File("a.txt"), new File("b.txt"));
     * IOUtil.forEachLine(files,
     *     IOUtil.LineIterationOptions.builder().readThreads(2).processThreads(4).build(),
     *     line -> process(line));
     * }</pre>
     *
     * <p>The {@code charset} of the supplied {@link LineIterationOptions} selects the encoding; it defaults
     * to UTF-8.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param files      the collection of files/directories to process; no element may be {@code null}. A directory contributes the regular files
     *        underneath it, recursively (links to files included; a linked directory is not descended into, and special
     *        files and dangling links are left out - see the class contract).
     * @param options    the slicing, concurrency and charset settings; {@code null} means "no slicing, read and
     *                   process on the calling thread, decode as UTF-8".
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if opening a file in {@code files} or reading its lines fails
     * @throws E if lineAction throws an exception while processing a line.
     * @see LineIterationOptions
     */
    public static <E extends Exception> void forEachLine(final Collection<File> files, final LineIterationOptions options,
            final Throwables.Consumer<? super String, E> lineAction) throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(files, options, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the given collection of files line by line, taking every slicing and concurrency setting from a named
     * {@link LineIterationOptions} builder instead of from positional numbers. See
     * {@link #forEachLine(File, LineIterationOptions, Throwables.Consumer)} for why this form is preferred.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOUtil.forEachLine(Arrays.asList(new File("a.txt"), new File("b.txt")),
     *     IOUtil.LineIterationOptions.builder().offset(10).count(100).build(),
     *     line -> process(line),
     *     () -> System.out.println("Done"));
     * }</pre>
     *
     * <p>The {@code charset} of the supplied {@link LineIterationOptions} selects the encoding; it defaults
     * to UTF-8.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param files      the collection of files/directories to process; no element may be {@code null}. A directory contributes the regular files
     *        underneath it, recursively (links to files included; a linked directory is not descended into, and special
     *        files and dangling links are left out - see the class contract).
     * @param options    the slicing, concurrency and charset settings; {@code null} means "no slicing, read and
     *                   process on the calling thread, decode as UTF-8".
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if opening a file in {@code files} or reading its lines fails
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see LineIterationOptions
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final Collection<File> files, final LineIterationOptions options,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        final LineIterationOptions opts = options == null ? DEFAULT_LINE_ITERATION_OPTIONS : options;

        doForEachLine(files, opts.offset(), opts.count(), opts.readThreads(), opts.processThreads(), opts.queueSize(), opts.charset(), lineAction, onComplete);
    }

    /**
     * Parses the specified InputStream line by line.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new ByteArrayInputStream("line1\nline2\nline3".getBytes(StandardCharsets.UTF_8))) {
     *     IOUtil.forEachLine(is, line -> System.out.println(line));
     * }
     * // Edge: empty stream
     * try (InputStream is = new ByteArrayInputStream(new byte[0])) {
     *     IOUtil.forEachLine(is, line -> System.out.println(line));
     * }
     * }</pre>
     *
     * <p>This overload decodes the stream as <b>UTF-8</b> and does not close it. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the InputStream to read lines from.
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forEachLine(final InputStream source, final Throwables.Consumer<? super String, E> lineAction)
            throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified InputStream line by line.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new ByteArrayInputStream("line1\nline2\nline3".getBytes(StandardCharsets.UTF_8))) {
     *     IOUtil.forEachLine(is,
     *         line -> System.out.println(line),
     *         () -> System.out.println("Done"));
     * }
     * // Edge: empty stream
     * try (InputStream is = new ByteArrayInputStream(new byte[0])) {
     *     IOUtil.forEachLine(is,
     *         line -> System.out.println(line),
     *         () -> System.out.println("Done"));
     * }
     * }</pre>
     *
     * <p>This overload decodes the stream as <b>UTF-8</b> and does not close it. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the InputStream to read lines from.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final InputStream source,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        forEachLine(source, 0, Long.MAX_VALUE, lineAction, onComplete);
    }

    /**
     * Parses the specified InputStream line by line.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new ByteArrayInputStream("line1\nline2\nline3".getBytes(StandardCharsets.UTF_8))) {
     *     IOUtil.forEachLine(is, 0, 10, line -> System.out.println(line));   // process first 10 lines
     * }
     * // Edge: skip 1 line
     * try (InputStream is = new ByteArrayInputStream("line1\nline2\nline3".getBytes(StandardCharsets.UTF_8))) {
     *     IOUtil.forEachLine(is, 1, 10, line -> System.out.println(line));
     * }
     * }</pre>
     *
     * <p>This overload decodes the stream as <b>UTF-8</b> and does not close it. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the InputStream to read lines from.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, if the source is {@code null}, or if a
     *                                  {@code lineOffset} or {@code count} argument is negative.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forEachLine(final InputStream source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction) throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, lineOffset, count, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified InputStream line by line.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new ByteArrayInputStream("line1\nline2\nline3".getBytes(StandardCharsets.UTF_8))) {
     *     IOUtil.forEachLine(is, 0, 10,
     *         line -> System.out.println(line),
     *         () -> System.out.println("Done"));
     * }
     * // Edge: skip 1 line
     * try (InputStream is = new ByteArrayInputStream("line1\nline2\nline3".getBytes(StandardCharsets.UTF_8))) {
     *     IOUtil.forEachLine(is, 1, 10,
     *         line -> System.out.println(line),
     *         () -> System.out.println("Done"));
     * }
     * }</pre>
     *
     * <p>This overload decodes the stream as <b>UTF-8</b> and does not close it. Pass a
     * {@link LineIterationOptions} to choose another charset.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the InputStream to read lines from.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, if the source is
     *                                  {@code null}, or if a {@code lineOffset} or {@code count} argument is negative.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final InputStream source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        doForEachLine(source, lineOffset, count, 0, 0, DEFAULT_CHARSET, lineAction, onComplete);
    }

    /**
     * Parses the specified {@code InputStream} line by line, taking every slicing and concurrency setting from a named
     * {@link LineIterationOptions} builder instead of from positional numbers. See
     * {@link #forEachLine(File, LineIterationOptions, Throwables.Consumer)} for why this form is preferred.
     *
     * <p>The stream is not closed by this method.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the {@code InputStream} to read lines from.
     * @param options    the slicing, concurrency and charset settings; {@code null} means "no slicing, read and
     *                   process on the calling thread, decode as UTF-8". {@code readThreads} is ignored: there is
     *                   only one source to read.
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @see LineIterationOptions
     */
    public static <E extends Exception> void forEachLine(final InputStream source, final LineIterationOptions options,
            final Throwables.Consumer<? super String, E> lineAction) throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, options, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified {@code InputStream} line by line, taking every slicing and concurrency setting from a named
     * {@link LineIterationOptions} builder instead of from positional numbers. See
     * {@link #forEachLine(File, LineIterationOptions, Throwables.Consumer)} for why this form is preferred.
     *
     * <p>The stream is not closed by this method.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the {@code InputStream} to read lines from.
     * @param options    the slicing, concurrency and charset settings; {@code null} means "no slicing, read and
     *                   process on the calling thread, decode as UTF-8". {@code readThreads} is ignored: there is
     *                   only one source to read.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see LineIterationOptions
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final InputStream source, final LineIterationOptions options,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        final LineIterationOptions opts = options == null ? DEFAULT_LINE_ITERATION_OPTIONS : options;

        doForEachLine(source, opts.offset(), opts.count(), opts.processThreads(), opts.queueSize(), opts.charset(), lineAction, onComplete);
    }

    /**
     * Parses the specified Reader line by line.
     *
     * <p><b>Reader position:</b> the reader is read to end of input - or, when a {@code count} stops the
     * iteration early, to an unspecified point past the last line processed. It is not closed, but it is not
     * meant to be continued from either; use {@link #readLines(Reader, int, int)} to take a bounded slice and
     * keep reading.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new StringReader("line1\nline2\nline3")) {
     *     IOUtil.forEachLine(reader, line -> System.out.println(line));
     * }
     * // Edge: empty reader
     * try (Reader reader = new StringReader("")) {
     *     IOUtil.forEachLine(reader, line -> System.out.println(line));
     * }
     * }</pre>
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the Reader to read lines from.
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forEachLine(final Reader source, final Throwables.Consumer<? super String, E> lineAction)
            throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified Reader line by line.
     *
     * <p><b>Reader position:</b> the reader is read to end of input - or, when a {@code count} stops the
     * iteration early, to an unspecified point past the last line processed. It is not closed, but it is not
     * meant to be continued from either; use {@link #readLines(Reader, int, int)} to take a bounded slice and
     * keep reading.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new StringReader("line1\nline2\nline3")) {
     *     IOUtil.forEachLine(reader,
     *         line -> System.out.println(line),
     *         () -> System.out.println("Done"));
     * }
     * // Edge: empty reader
     * try (Reader reader = new StringReader("")) {
     *     IOUtil.forEachLine(reader,
     *         line -> System.out.println(line),
     *         () -> System.out.println("Done"));
     * }
     * }</pre>
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the Reader to read lines from.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final Reader source, final Throwables.Consumer<? super String, E> lineAction,
            final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        forEachLine(source, 0, Long.MAX_VALUE, lineAction, onComplete);
    }

    /**
     * Parses the specified Reader line by line.
     *
     * <p><b>Reader position:</b> the reader is read to end of input - or, when a {@code count} stops the
     * iteration early, to an unspecified point past the last line processed. It is not closed, but it is not
     * meant to be continued from either; use {@link #readLines(Reader, int, int)} to take a bounded slice and
     * keep reading.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new StringReader("line1\nline2\nline3")) {
     *     IOUtil.forEachLine(reader, 0, 10, line -> System.out.println(line));   // process first 10 lines
     * }
     * // Edge: skip 1 line
     * try (Reader reader = new StringReader("line1\nline2\nline3")) {
     *     IOUtil.forEachLine(reader, 1, 10, line -> System.out.println(line));
     * }
     * }</pre>
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the Reader to read lines from.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, if the source is {@code null}, or if a
     *                                  {@code lineOffset} or {@code count} argument is negative.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception> void forEachLine(final Reader source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction) throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, lineOffset, count, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified Reader line by line.
     *
     * <p><b>Reader position:</b> the reader is read to end of input - or, when a {@code count} stops the
     * iteration early, to an unspecified point past the last line processed. It is not closed, but it is not
     * meant to be continued from either; use {@link #readLines(Reader, int, int)} to take a bounded slice and
     * keep reading.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new StringReader("line1\nline2\nline3")) {
     *     IOUtil.forEachLine(reader, 0, 10,
     *         line -> System.out.println(line),
     *         () -> System.out.println("Done"));
     * }
     * // Edge: skip 1 line
     * try (Reader reader = new StringReader("line1\nline2\nline3")) {
     *     IOUtil.forEachLine(reader, 1, 10,
     *         line -> System.out.println(line),
     *         () -> System.out.println("Done"));
     * }
     * }</pre>
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the Reader to read lines from.
     * @param lineOffset the number of lines to skip from the beginning.
     * @param count      the maximum number of lines to process after the offset.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, if the source is
     *                                  {@code null}, or if a {@code lineOffset} or {@code count} argument is negative.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E                    if lineAction throws an exception while processing a line.
     * @throws E2                   if onComplete throws an exception.
     * @see #forEachLine(Reader, LineIterationOptions, Throwables.Consumer, Throwables.Runnable)
     * @see Iterators#forEach(Iterator, Iterators.IterateOptions, Throwables.Consumer, Throwables.Runnable)
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final Reader source, final long lineOffset, final long count,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        doForEachLine(source, lineOffset, count, 0, 0, lineAction, onComplete);
    }

    /**
     * Parses the specified {@code Reader} line by line, taking every slicing and concurrency setting from a named
     * {@link LineIterationOptions} builder instead of from positional numbers. See
     * {@link #forEachLine(File, LineIterationOptions, Throwables.Consumer)} for why this form is preferred.
     *
     * <p>The reader is not closed by this method, but it is read to end of input - or, when {@code count}
     * stops the iteration early, to an unspecified point past the last line processed - so it is not meant to
     * be continued from.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param source     the {@code Reader} to read lines from.
     * @param options    the slicing and concurrency settings; {@code null} means "no slicing, read and process on
     *                   the calling thread". Two fields do not apply here and are ignored: {@code readThreads},
     *                   because there is only one source to read, and {@code charset}, because a {@code Reader}
     *                   already yields decoded characters - set the charset when you construct the reader.
     * @param lineAction the action to perform on each line.
     * @throws IllegalArgumentException if {@code lineAction} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @see LineIterationOptions
     */
    public static <E extends Exception> void forEachLine(final Reader source, final LineIterationOptions options,
            final Throwables.Consumer<? super String, E> lineAction) throws IllegalArgumentException, UncheckedIOException, E {
        N.checkArgNotNull(lineAction, cs.lineAction);

        forEachLine(source, options, lineAction, Fn.emptyAction());
    }

    /**
     * Parses the specified {@code Reader} line by line, taking every slicing and concurrency setting from a named
     * {@link LineIterationOptions} builder instead of from positional numbers. See
     * {@link #forEachLine(File, LineIterationOptions, Throwables.Consumer)} for why this form is preferred.
     *
     * <p>The reader is not closed by this method, but it is read to end of input - or, when {@code count}
     * stops the iteration early, to an unspecified point past the last line processed - so it is not meant to
     * be continued from.
     *
     * @param <E>        the type of exception that the lineAction may throw during line processing.
     * @param <E2>       the type of exception that the onComplete callback may throw after all lines are processed.
     * @param source     the {@code Reader} to read lines from.
     * @param options    the slicing and concurrency settings; {@code null} means "no slicing, read and process on
     *                   the calling thread". Two fields do not apply here and are ignored: {@code readThreads},
     *                   because there is only one source to read, and {@code charset}, because a {@code Reader}
     *                   already yields decoded characters - set the charset when you construct the reader.
     * @param lineAction the action to perform on each line.
     * @param onComplete the action to perform after all lines have been processed successfully.
     * @throws IllegalArgumentException if any of {@code lineAction}, {@code onComplete} is {@code null}, or if the source is {@code null}.
     * @throws UncheckedIOException if reading lines from {@code source} fails
     * @throws E if lineAction throws an exception while processing a line.
     * @throws E2 if onComplete throws an exception.
     * @see LineIterationOptions
     */
    public static <E extends Exception, E2 extends Exception> void forEachLine(final Reader source, final LineIterationOptions options,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        final LineIterationOptions opts = options == null ? DEFAULT_LINE_ITERATION_OPTIONS : options;

        // charset is ignored here: a Reader already yields decoded characters.
        doForEachLine(source, opts.offset(), opts.count(), opts.processThreads(), opts.queueSize(), lineAction, onComplete);
    }

    /**
     * Shared implementation for every {@code forEachLine(Collection<File>, ..)} overload, so that no public
     * entry point has to route through another one.
     * @throws IllegalArgumentException if {@code files}, {@code lineAction}, or {@code onComplete} is null,
     *         or an offset, count, thread count, or queue size is negative
     * @throws UncheckedIOException if a source file cannot be opened or read
     * @throws E if {@code lineAction} throws while processing a line
     * @throws E2 if {@code onComplete} throws after all selected lines have been processed
     */
    private static <E extends Exception, E2 extends Exception> void doForEachLine(final Collection<File> files, final long lineOffset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Charset charset,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgument(lineOffset >= 0 && count >= 0, "'lineOffset'=%s and 'count'=%s cannot be negative", lineOffset, count);
        N.checkArgument(readThreadNum >= 0 && processThreadNum >= 0 && queueSize >= 0,
                "'readThreadNum'=%s, 'processThreadNum'=%s and 'queueSize'=%s cannot be negative", readThreadNum, processThreadNum, queueSize);
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);
        // A null collection is a programming error, not "read no files": zip(Collection, ..) and
        // merge(Collection, ..) both reject one, and treating it as empty made forEachLine the single
        // collection-taking operation that answered a missing argument with a silent no-op.
        N.checkArgNotNull(files, cs.files);

        if (N.isEmpty(files)) {
            // No files to read still counts as successful completion, so honor the onComplete callback
            // (consistent with the non-empty path, where Iterators.forEach runs it after the last line).
            onComplete.run();

            return;
        }

        // Not named "cs": that is the parameter-name constants class, used above.
        final Charset lineCharset = checkCharset(charset);

        // Iterators open their file lazily (see LazyFileLineIterator): at most readThreadNum files are open
        // concurrently instead of one descriptor per file up front, so a directory with a very large number
        // of files no longer exhausts the process's file descriptors.
        final List<LazyFileLineIterator> iterators = new ArrayList<>(files.size());
        // On the parallel path Iterators.forEach abandons its workers after a bounded cancellation wait (an
        // interrupt, or a failed submission), and an abandoned worker may still be inside a read: closing that
        // reader would pin this thread behind it (see LazyFileLineIterator.release). The readers are closed only
        // when no thread can still own them: single-threaded, or after a normal return with no reader threads -
        // process workers are joined before a normal return, but a reader thread is only told to stop, and a
        // counted read returns while it may still be inside readLine() on a source that blocks. Otherwise only
        // the file handles are released - which is what a close is for.
        boolean readersFree = readThreadNum == 0 && processThreadNum == 0;

        try { //NOSONAR
            for (final File subFile : files) {
                checkLineSource(subFile, cs.file);

                // A directory is expanded; anything else that exists - a regular file, and also a FIFO or a
                // character device such as /dev/stdin - is read as it is. See checkLineSource(..).
                if (subFile.isDirectory()) {
                    for (final File subSubFile : listFiles(subFile, true, readable_entries_filter)) {
                        iterators.add(new LazyFileLineIterator(subSubFile, lineCharset));
                    }
                } else {
                    iterators.add(new LazyFileLineIterator(subFile, lineCharset));
                }
            }

            Iterators.forEach(iterators,
                    Iterators.IterateOptions.builder()
                            .offset(lineOffset)
                            .count(count)
                            .readThreads(readThreadNum)
                            .processThreads(processThreadNum)
                            .queueSize(queueSize)
                            .build(),
                    lineAction, onComplete);
            readersFree = readThreadNum == 0;
        } finally {
            for (final LazyFileLineIterator iter : iterators) {
                iter.release(readersFree);
            }
        }
    }

    /**
     * Shared implementation for every {@code forEachLine(File, ..)} overload: validates the source and expands a
     * directory into its files before handing off to the collection core.
     * @throws IllegalArgumentException if {@code source}, {@code lineAction}, or {@code onComplete} is null,
     *         or an offset, count, thread count, or queue size is negative
     * @throws UncheckedIOException if the source cannot be inspected, opened, or read
     * @throws E if {@code lineAction} throws while processing a line
     * @throws E2 if {@code onComplete} throws after all selected lines have been processed
     */
    private static <E extends Exception, E2 extends Exception> void doForEachLine(final File source, final long lineOffset, final long count,
            final int readThreadNum, final int processThreadNum, final int queueSize, final Charset charset,
            final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(source, cs.source);
        N.checkArgument(lineOffset >= 0 && count >= 0, "'lineOffset'=%s and 'count'=%s cannot be negative", lineOffset, count);
        N.checkArgument(readThreadNum >= 0 && processThreadNum >= 0 && queueSize >= 0,
                "'readThreadNum'=%s, 'processThreadNum'=%s and 'queueSize'=%s cannot be negative", readThreadNum, processThreadNum, queueSize);
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        checkLineSource(source, cs.source);

        doForEachLine(source.isDirectory() ? listFiles(source, true, readable_entries_filter) : Array.asList(source), lineOffset, count, readThreadNum,
                processThreadNum, queueSize, charset, lineAction, onComplete);
    }

    /**
     * Validates a {@code forEachLine} source the way the {@code read*} family validates one: it has to exist and be
     * readable, and that is all.
     *
     * <p>{@code checkFileExists(source, true)} was used here before, and it additionally rejects anything that is
     * neither a regular file nor a directory. So {@code forEachLine(new File("/dev/stdin"), ..)}, or a FIFO, failed
     * with "exists but is neither a file nor a directory" while {@code readAllLines(..)} over the same path read
     * it - and the class contract calls {@code forEachLine} the streaming form of {@code readAllLines}. A directory
     * is expanded by the caller; anything else is handed to {@link LazyFileLineIterator}, whose open reports a
     * source that cannot actually be read.
     *
     * @param source the file, directory or special file to read lines from.
     * @param argName the caller's name for it, used in the {@code IllegalArgumentException} message.
     * @throws IllegalArgumentException if {@code source} is {@code null}.
     * @throws UncheckedIOException wrapping a {@link FileNotFoundException} if {@code source} does not exist or
     *         cannot be read.
     */
    private static void checkLineSource(final File source, final String argName) throws IllegalArgumentException, UncheckedIOException {
        N.checkArgNotNull(source, argName);

        if (!source.exists()) {
            throw new UncheckedIOException(new FileNotFoundException("'" + describe(source) + "' does not exist"));
        }

        if (!source.canRead()) {
            throw new UncheckedIOException(new FileNotFoundException("'" + describe(source) + "' exists but cannot be read"));
        }
    }

    /**
     * Shared implementation for every {@code forEachLine(Reader, ..)} overload.
     * @throws IllegalArgumentException if {@code source}, {@code lineAction}, or {@code onComplete} is null,
     *         or an offset, count, thread count, or queue size is negative
     * @throws UncheckedIOException if reading a line from {@code source} fails
     * @throws E if {@code lineAction} throws while processing a line
     * @throws E2 if {@code onComplete} throws after all selected lines have been processed
     */
    private static <E extends Exception, E2 extends Exception> void doForEachLine(final Reader source, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Throwables.Consumer<? super String, E> lineAction, final Throwables.Runnable<E2> onComplete)
            throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(source, cs.source); // before the shield below wraps it: a null source is a bad argument, not an NPE
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        // LineIterator closes its reader when a read fails; the caller's reader is shielded so that the
        // "not closed by this method" promise holds on the failure path as well as on the success path.
        Iterators.forEach(new LineIterator(shieldFromClose(source)),
                Iterators.IterateOptions.builder().offset(lineOffset).count(count).processThreads(processThreadNum).queueSize(queueSize).build(), lineAction,
                onComplete);
    }

    /**
     * Shared implementation for every {@code forEachLine(InputStream, ..)} overload.
     * @throws IllegalArgumentException if {@code source}, {@code lineAction}, or {@code onComplete} is null,
     *         or an offset, count, thread count, or queue size is negative
     * @throws UncheckedIOException if reading or decoding a line from {@code source} fails
     * @throws E if {@code lineAction} throws while processing a line
     * @throws E2 if {@code onComplete} throws after all selected lines have been processed
     */
    private static <E extends Exception, E2 extends Exception> void doForEachLine(final InputStream source, final long lineOffset, final long count,
            final int processThreadNum, final int queueSize, final Charset charset, final Throwables.Consumer<? super String, E> lineAction,
            final Throwables.Runnable<E2> onComplete) throws IllegalArgumentException, UncheckedIOException, E, E2 {
        N.checkArgNotNull(source, cs.source); // before the shield below wraps it: a null source is a bad argument, not an NPE
        N.checkArgNotNull(lineAction, cs.lineAction);
        N.checkArgNotNull(onComplete, cs.onComplete);

        // Decode with the caller's charset. The decoder is not closed on the success path (Objectory.recycle(..)
        // does not close the underlying source), and when a read fails LineIterator closes the pooled reader -
        // which would close the decoder and, through it, the caller's stream - so the stream is shielded.
        final BufferedReader br = Objectory.createBufferedReader(IOUtil.newInputStreamReader(shieldFromClose(source), charset));

        // On the parallel path Iterators.forEach abandons its workers after a bounded cancellation wait
        // (an interrupt, or a failed submission), and an abandoned worker may still be inside br.readLine().
        // Recycling the reader then either blocks on the decoder's monitor until that read returns - on
        // JDK 25 the interrupted caller was pinned for the whole remaining read, which on a socket or pipe
        // is unbounded - or, where the decoder uses an internal lock (JDK 19-24), hands a reader the worker
        // is still filling to the next pool user, which then reads the abandoned stream's bytes. So the
        // reader goes back to the pool only when no worker can still own it: on the single-threaded path,
        // or after a normal return, by which point every worker has exited. Otherwise it is simply dropped;
        // it holds nothing but a buffer and the shielded, caller-owned stream, which is never closed here.
        boolean safeToRecycle = processThreadNum == 0;

        try {
            doForEachLine(br, lineOffset, count, processThreadNum, queueSize, lineAction, onComplete);
            safeToRecycle = true;
        } finally {
            if (safeToRecycle) {
                Objectory.recycle(br);
            }
        }
    }

    /**
     * A view of the caller's reader whose {@code close()} does nothing, for the line iteration engine.
     *
     * <p>{@link LineIterator#hasNext()} closes its reader when a read fails. Every {@code forEachLine(Reader, ..)}
     * overload promises not to close the caller's reader, and on the success path none of them did; on the failure
     * path the iterator closed it. A caller-owned {@link java.io.BufferedReader} is still read through directly - the
     * view is itself a {@code BufferedReader} that delegates every read, so the iterator adds no buffer of its own -
     * and any other reader is wrapped exactly as before.
     *
     * @param source the caller's reader; must not be {@code null}.
     * @return a reader over {@code source} that ignores {@code close()}.
     */
    private static Reader shieldFromClose(final Reader source) {
        return source instanceof java.io.BufferedReader ? new NonClosingBufferedReader((java.io.BufferedReader) source) : new NonClosingReader(source);
    }

    /**
     * A view of the caller's stream whose {@code close()} does nothing (see {@link #shieldFromClose(Reader)}).
     *
     * @param source the caller's stream; must not be {@code null}.
     * @return a stream over {@code source} that ignores {@code close()}.
     */
    private static InputStream shieldFromClose(final InputStream source) {
        return new java.io.FilterInputStream(source) {
            @Override
            public void close() {
                // the caller owns the source
            }
        };
    }

    /**
     * {@link #shieldFromClose(Reader)} for a reader that is not buffered.
     */
    private static final class NonClosingReader extends java.io.FilterReader {
        NonClosingReader(final Reader in) {
            super(in);
        }

        @Override
        public void close() {
            // the caller owns the source
        }
    }

    /**
     * {@link #shieldFromClose(Reader)} for a reader that is already a {@link java.io.BufferedReader}: every read is
     * delegated, so the caller's buffer keeps being the only one.
     */
    private static final class NonClosingBufferedReader extends java.io.BufferedReader {
        private final java.io.BufferedReader delegate;

        NonClosingBufferedReader(final java.io.BufferedReader delegate) {
            super(delegate, 1);
            this.delegate = delegate;
        }

        @Override
        public String readLine() throws IOException {
            return delegate.readLine();
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public int read(final char[] cbuf, final int off, final int len) throws IOException {
            return delegate.read(cbuf, off, len);
        }

        @Override
        public long skip(final long n) throws IOException {
            return delegate.skip(n);
        }

        @Override
        public boolean ready() throws IOException {
            return delegate.ready();
        }

        @Override
        public boolean markSupported() {
            return delegate.markSupported();
        }

        @Override
        public void mark(final int readAheadLimit) throws IOException {
            delegate.mark(readAheadLimit);
        }

        @Override
        public void reset() throws IOException {
            delegate.reset();
        }

        @Override
        public void close() {
            // the caller owns the source
        }
    }

    /**
     * Opens {@code source} (including {@code .gz}/{@code .zip} file-name handling) and applies {@code action}.
     * The stream and any accompanying {@link ZipFile} are closed afterwards; close failures are added as
     * suppressed exceptions when {@code action} already failed.
     * @throws IllegalArgumentException if {@code source} is {@code null} or is a directory.
     * @throws IOException if opening, reading, or closing the file or compressed-file resources fails.
     * @throws E if {@code action} throws an exception.
     */
    private static <R, E extends Exception> R withOpenedFile(final File source, final Throwables.Function<? super InputStream, ? extends R, E> action)
            throws IllegalArgumentException, IOException, E {
        // The single validation point for the whole file-based read family: without it a null source failed
        // with NullPointerException from openFile's source.getName(), which the class contract reports as
        // IllegalArgumentException.
        N.checkArgNotNull(source, cs.source);

        final Holder<ZipFile> zipHolder = new Holder<>();
        final InputStream is;

        try {
            is = openFile(source, zipHolder);
        } catch (final IOException e) {
            throw classifyFailedOpen(source, e);
        }

        try (final ZipFile zipFile = zipHolder.value();
             final InputStream in = is) {
            return action.apply(in);
        }
    }

    /**
     * Opens a file and returns an input stream. File-name checks for {@code .gz} and {@code .zip}
     * are case-insensitive; ZIP files are read from their first non-directory entry.
     *
     * @param source the file to open, must not be {@code null}.
     * @param outputZipFile a holder for the ZipFile if the source is a .zip file.
     * @return an InputStream for reading the file.
     * @throws IOException if opening {@code source} , reading its compressed header, or opening its first ZIP file entry fails, or the ZIP
     *         archive has no file entries
     */
    private static InputStream openFile(final File source, final Holder<ZipFile> outputZipFile) throws IOException {
        return openFile(source, outputZipFile, null);
    }

    /**
     * Opens a file and returns an input stream. File-name checks for {@code .gz} and {@code .zip}
     * are case-insensitive; ZIP files are read from their first non-directory entry.
     *
     * @param source the file to open, must not be {@code null}.
     * @param outputZipFile a holder for the ZipFile if the source is a .zip file.
     * @param sampleScale optional; when given, it is filled in with a byte counter and the total that counter is
     *        measured against, so that a caller sampling the head of the stream can scale up to the whole source.
     * @return an InputStream for reading the file.
     * @throws IOException if opening {@code source} , reading its compressed header, or opening its first ZIP file entry fails, or the ZIP
     *         archive has no file entries
     */
    private static InputStream openFile(final File source, final Holder<ZipFile> outputZipFile, final SampleScale sampleScale) throws IOException {
        InputStream is = null;
        final String lowerCaseName = source.getName().toLowerCase(Locale.ROOT);

        if (lowerCaseName.endsWith(GZ)) {
            final FileInputStream fis = openFileInputStream(source);

            try {
                // The counter goes UNDER the decompressor, so it measures compressed bytes taken from the file -
                // the same unit as file.length(). Counting above it would measure decompressed bytes and make any
                // scaling against the file length wrong by the whole compression ratio.
                is = new GZIPInputStream(scaleBy(fis, sampleScale, source.length()));
            } catch (final Throwable e) {
                closeSuppressing(fis, e);
                throw e;
            }
        } else if (lowerCaseName.endsWith(ZIP)) {
            final ZipFile zf = openZipFile(source);

            try {
                final java.util.Enumeration<? extends ZipEntry> entries = zf.entries();
                ZipEntry ze = null;

                while (entries.hasMoreElements()) {
                    final ZipEntry candidate = entries.nextElement();

                    if (!candidate.isDirectory()) {
                        ze = candidate;
                        break;
                    }
                }

                if (ze == null) {
                    throw new IOException("ZIP file contains no file entries: " + describe(source));
                }

                // A ZipFile is random-access, so there is no raw stream to count under the entry. Count the
                // entry's decompressed bytes instead and scale against its declared uncompressed size, which the
                // central directory already carries (-1 when unknown, which simply disables scaling).
                is = scaleBy(zf.getInputStream(ze), sampleScale, ze.getSize());
                outputZipFile.setValue(zf);
            } catch (final Throwable e) {
                closeSuppressing(zf, e);
                throw e;
            }
        } else {
            is = scaleBy(openFileInputStream(source), sampleScale, source.length());
        }

        return is;
    }

    /**
     * Opens a {@code .zip} source for the {@code read*} family, reporting an absent file the way the class contract
     * and the sibling {@code .gz}/plain branches do.
     *
     * <p>Those branches open through {@link FileInputStream}, which raises {@link FileNotFoundException} for a file
     * that is not there. {@link ZipFile} does not: since the JDK moved it onto NIO it raises
     * {@link NoSuchFileException} for the same file, so {@code readAllBytes(new File("x.zip"))} was the one spelling
     * of a missing source that a {@code catch (FileNotFoundException e)} written against the contract did not see.
     * The translation carries the original as the cause. Every other failure - a directory of that name, an archive
     * that cannot be parsed - passes through untouched and is classified by the caller as before.
     *
     * @param source the {@code .zip} file to open.
     * @return the open archive.
     * @throws FileNotFoundException if {@code source} does not exist.
     * @throws IOException if the archive cannot be opened for any other reason.
     */
    private static ZipFile openZipFile(final File source) throws IOException {
        try {
            return new ZipFile(source);
        } catch (final NoSuchFileException e) {
            final FileNotFoundException absent = new FileNotFoundException("'" + describe(source) + "' does not exist");
            absent.initCause(e);

            throw absent;
        }
    }

    /**
     * Converts a CharSequence to a byte array using the specified charset.
     *
     * @param cs      the CharSequence to convert.
     * @param charset the charset to use for encoding; if {@code null}, the default charset (UTF-8) is used.
     * @return the byte array representation of the CharSequence.
     */
    private static byte[] toByteArray(final CharSequence cs, final Charset charset) {
        return String.valueOf(cs).getBytes(checkCharset(charset));
    }

    /**
     * Immutable options for the {@link IOUtil#forEachLine(File, LineIterationOptions, Throwables.Consumer)} family: the
     * slicing and concurrency knobs of {@link Iterators.IterateOptions}, plus the {@code charset} the source is
     * decoded with.
     *
     * <p>This is the only way to give {@code forEachLine} a charset, and the only way to ask it for concurrency. The
     * positional overloads cover {@code offset}/{@code count} alone and always read UTF-8 on the calling thread.</p>
     *
     * <p><b>Why this is not an extension of {@code Iterators.IterateOptions}:</b> a charset is a decoding concern,
     * and {@code Iterators} iterates elements of any type. A separate class also lets
     * {@code Iterators.IterateOptions} stay a {@code final} value type, and the deliberately distinct <i>name</i>
     * matters as much as the separation: had both been called {@code IterateOptions}, they could not be imported
     * into one file, and passing the charset-less one would have compiled and silently meant UTF-8 instead of
     * failing.</p>
     *
     * <p><b>Field meanings.</b> {@code offset} and {@code count} slice the combined line stream before processing.
     * {@code readThreads} enables dedicated reader workers; it is honoured by the {@code File} and
     * {@code Collection<File>} overloads, while the {@code InputStream} and {@code Reader} forms have a single
     * already-open source and ignore it. {@code processThreads} controls concurrent calls to the line action.
     * Without dedicated readers, processing workers also read the source; reading stays on the calling thread
     * only when both effective reader and processing thread counts are zero.
     * {@code queueSize} sizes the hand-off buffer between the reader threads and the line action, so it only
     * takes effect when {@code readThreads > 0}; {@code 0} lets the implementation choose. {@code charset}
     * decodes the source and is ignored by the {@code Reader} overloads, which receive characters that are
     * already decoded.</p>
     *
     * <p><b>What {@code readThreads} buys, and what it costs.</b> Across several files it reads them
     * concurrently. Over a <i>single</i> {@code File} it cannot do that - there is one source - but it is not
     * ignored either: reading still moves to a worker that fills the hand-off queue ahead of the line action, so
     * a slow action overlaps with I/O instead of alternating with it. Either way the reading happens on the
     * library's shared stream pool, so use it when the action is the bottleneck, not as a default.
     *
     * <p>{@code processThreads} maps onto threads created for the call and awaited on normal completion.
     * Exceptional cancellation waits for a bounded period, so a worker that ignores interruption may outlive the call.
     * {@code readThreads} borrows from a pool whose workers stay parked for a keep-alive period afterwards; they
     * are daemon threads, so they do not hold up JVM shutdown, but they are shared, so a call that asks for more
     * readers than there are files simply leaves the extra ones idle.</p>
     *
     * <p>All values default to "no slicing, caller-thread reading and processing, UTF-8": {@code offset = 0},
     * {@code count = Long.MAX_VALUE}, {@code readThreads = 0}, {@code processThreads = 0}, {@code queueSize = 0}
     * and {@code charset = UTF-8}. A {@code null} {@code charset} is also read as UTF-8.</p>
     *
     * <p>Every numeric field must be non-negative; {@code build()} rejects a negative one with an
     * {@link IllegalArgumentException} naming it. Validating here keeps the complaint next to the value that
     * caused it: it used to surface only once the options reached a {@code forEachLine} call, which is often
     * far from where they were assembled.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // a Latin-1 log, 4 worker threads, skipping the first 100 lines
     * IOUtil.forEachLine(new File("app.log"),
     *     IOUtil.LineIterationOptions.builder()
     *         .charset(StandardCharsets.ISO_8859_1)
     *         .offset(100)
     *         .processThreads(4)
     *         .build(),
     *     line -> process(line));
     * }</pre>
     *
     * @see IOUtil#forEachLine(File, LineIterationOptions, Throwables.Consumer)
     * @see IOUtil#forEachLine(Collection, LineIterationOptions, Throwables.Consumer)
     * @see IOUtil#forEachLine(InputStream, LineIterationOptions, Throwables.Consumer)
     * @see IOUtil#forEachLine(Reader, LineIterationOptions, Throwables.Consumer)
     * @see Iterators.IterateOptions
     */
    // The five slicing/concurrency fields deliberately mirror Iterators.IterateOptions rather than extending it -
    // see the class note above. The doForEachLine(..) cores earlier in this file name all six, so they are the one
    // place both types have to be kept in step.
    @Builder
    @Value
    @Accessors(fluent = true)
    public static final class LineIterationOptions {
        /**
         * Number of lines to skip before the first one is passed to the action. Non-negative; defaults to 0.
         */
        @Builder.Default
        private long offset = 0;

        /**
         * Maximum number of lines to pass to the action. Non-negative; defaults to {@link Long#MAX_VALUE}.
         */
        @Builder.Default
        private long count = Long.MAX_VALUE;

        /**
         * Number of dedicated reader threads. {@code 0} (the default) lets the processing workers read the source,
         * or reads on the calling thread when {@code processThreads} is also {@code 0}. Honoured
         * by the {@code File} and {@code Collection<File>} overloads; ignored by the {@code InputStream} and
         * {@code Reader} ones.
         */
        @Builder.Default
        private int readThreads = 0;

        /**
         * Number of threads that call the line action. {@code 0} (the default) calls it on the calling thread,
         * which is also the only setting under which the action's own checked exception type is preserved.
         */
        @Builder.Default
        private int processThreads = 0;

        /**
         * Size of the hand-off buffer between the reader threads and the line action. Only has an effect when
         * {@code readThreads > 0}; {@code 0} (the default) lets the implementation choose.
         */
        @Builder.Default
        private int queueSize = 0;

        /**
         * Charset the source is decoded with. Defaults to UTF-8, which {@code null} also means. Ignored by the
         * {@code Reader} overloads, which receive characters that are already decoded. The stored charset is never {@code null}.
         */
        @Builder.Default
        private Charset charset = DEFAULT_CHARSET;

        /**
         * Declared explicitly so that the builder validates: Lombok skips generating the all-args constructor
         * when one is present and has {@code build()} call this one instead. Without it a negative value was
         * accepted by {@code build()} and only complained about later, from inside whichever
         * {@code forEachLine(..)} overload the options were eventually handed to - and under a different name
         * there ({@code 'lineOffset'} on the file paths, {@code 'offset'} on the reader path) than the builder
         * field the caller had actually set.
         *
         * @param offset the number of lines to skip; must be non-negative.
         * @param count the maximum number of lines to process; must be non-negative.
         * @param readThreads the number of reader threads; must be non-negative.
         * @param processThreads the number of worker threads; must be non-negative.
         * @param queueSize the buffer size between reading and processing; must be non-negative.
         * @param charset the charset the source is decoded with; {@code null} is stored as UTF-8, so
         *        the stored charset is never {@code null}.
         * @throws IllegalArgumentException if any numeric argument is negative.
         */
        private LineIterationOptions(final long offset, final long count, final int readThreads, final int processThreads, final int queueSize,
                final Charset charset) throws IllegalArgumentException {
            N.checkArgNotNegative(offset, cs.offset);
            N.checkArgNotNegative(count, cs.count);
            N.checkArgNotNegative(readThreads, cs.readThreads);
            N.checkArgNotNegative(processThreads, cs.processThreads);
            N.checkArgNotNegative(queueSize, cs.queueSize);

            this.offset = offset;
            this.count = count;
            this.readThreads = readThreads;
            this.processThreads = processThreads;
            this.queueSize = queueSize;
            // Normalised here, not only at the point of use: this is a value type, and {@code charset()} used
            // to hand back the null the class javadoc says cannot be observed.
            this.charset = charset == null ? DEFAULT_CHARSET : charset;
        }
    }
}
