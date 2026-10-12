package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for IOUtil. */
@Tag("slow-test")
public class IOUtilPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G031 end ----
    // ---- perf review 2026-10-08 G032 begin ----
    @Nested
    class G032 {

        /** The pooled read buffer IOUtil's stream reads start in (16 KB on heaps up to 16 GB). */
        private final int bufferSize = Objectory.BUFFER_SIZE;

        /**
         * Delivers {@code data} in seeded pseudo-random chunks, may answer 0, may fail or break the read contract at a
         * chosen call, and folds every read call (destination length, offset, requested length, answer) into a digest.
         */
        private final class ScriptedInput extends java.io.InputStream {
            private final byte[] data;
            private final Random random;
            private final int maxChunk;
            private final int zeroPercent;
            private final int failAtCall;
            private final int anomalyAtCall;
            private final int anomalyDelta;
            final java.io.IOException failure = new java.io.IOException("scripted failure");
            int position;
            int calls;
            long digest = 17;

            ScriptedInput(final byte[] data, final long seed, final int maxChunk, final int zeroPercent, final int failAtCall, final int anomalyAtCall,
                    final int anomalyDelta) {
                this.data = data;
                this.random = new Random(seed);
                this.maxChunk = maxChunk;
                this.zeroPercent = zeroPercent;
                this.failAtCall = failAtCall;
                this.anomalyAtCall = anomalyAtCall;
                this.anomalyDelta = anomalyDelta;
            }

            @Override
            public int read() {
                throw new UnsupportedOperationException("single-byte read is not expected");
            }

            @Override
            public int read(final byte[] b, final int off, final int len) throws java.io.IOException {
                calls++;
                digest = ((digest * 31 + b.length) * 31 + off) * 31 + len;

                if (calls == failAtCall) {
                    throw failure;
                }

                final int answer;

                if (calls == anomalyAtCall) {
                    // Contract-breaking count: an over-report (nothing written) or a negative value other than -1.
                    answer = anomalyDelta >= 0 ? len + anomalyDelta : anomalyDelta;
                } else if (len == 0 || random.nextInt(100) < zeroPercent) {
                    answer = 0;
                } else if (position >= data.length) {
                    answer = -1;
                } else {
                    answer = Math.min(len, Math.min(data.length - position, 1 + random.nextInt(maxChunk)));
                    System.arraycopy(data, position, b, off, answer);
                    position += answer;
                }

                digest = digest * 31 + answer;

                return answer;
            }
        }

        private List<Object> outcome(final ScriptedInput input, final java.util.function.Function<ScriptedInput, String> operation) {
            final List<Object> outcome = new ArrayList<>();

            try {
                outcome.add(operation.apply(input));
            } catch (final RuntimeException e) {
                outcome.add(e.getClass().getName());
                outcome.add(e.getMessage());
                outcome.add(e.getCause() == null ? null : e.getCause().getClass().getName());
                outcome.add(e.getCause() == input.failure);
            }

            outcome.add(input.calls);
            outcome.add(input.digest);
            outcome.add(input.position);

            return outcome;
        }

        /** The pre-change implementation of readAllToString(InputStream, Charset): read all bytes, then decode them. */
        private String reference(final java.io.InputStream input, final java.nio.charset.Charset charset) {
            final byte[] bytes = IOUtil.readAllBytes(input);

            return new String(bytes, charset == null ? java.nio.charset.StandardCharsets.UTF_8 : charset);
        }

        private void assertSameAsReference(final byte[] data, final java.nio.charset.Charset charset, final long seed, final int maxChunk,
                final int zeroPercent, final int failAtCall, final int anomalyAtCall, final int anomalyDelta) {
            final List<Object> expected = outcome(new ScriptedInput(data, seed, maxChunk, zeroPercent, failAtCall, anomalyAtCall, anomalyDelta),
                    in -> reference(in, charset));
            final List<Object> actual = outcome(new ScriptedInput(data, seed, maxChunk, zeroPercent, failAtCall, anomalyAtCall, anomalyDelta),
                    in -> IOUtil.readAllToString(in, charset));

            assertEquals(expected, actual, () -> "length=" + data.length + " charset=" + charset + " seed=" + seed + " maxChunk=" + maxChunk + " zero%="
                    + zeroPercent + " failAt=" + failAtCall + " anomalyAt=" + anomalyAtCall + "/" + anomalyDelta);
        }

        private byte[] content(final int kind, final int length, final long seed) {
            final byte[] result = new byte[length];

            if (kind == 0) {
                for (int i = 0; i < length; i++) {
                    result[i] = (byte) (i % 61 == 60 ? '\n' : 'a' + i % 26);
                }
            } else if (kind == 1) {
                // 'a', e-acute (2 bytes), euro sign (3 bytes), U+1F600 (4 bytes), cut at length: sequences straddle every boundary.
                final byte[] pattern = { 'a', (byte) 0xC3, (byte) 0xA9, (byte) 0xE2, (byte) 0x82, (byte) 0xAC, (byte) 0xF0, (byte) 0x9F, (byte) 0x98,
                        (byte) 0x80 };

                for (int i = 0; i < length; i++) {
                    result[i] = pattern[i % pattern.length];
                }
            } else {
                new Random(seed).nextBytes(result);
            }

            return result;
        }

        private List<java.nio.charset.Charset> charsets() {
            final List<java.nio.charset.Charset> charsets = new ArrayList<>(Arrays.asList(java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.charset.StandardCharsets.ISO_8859_1, java.nio.charset.StandardCharsets.UTF_16, null));

            if (java.nio.charset.Charset.isSupported("windows-1252")) {
                charsets.add(java.nio.charset.Charset.forName("windows-1252"));
            }

            return charsets;
        }

        // G032-01: readAllToString(InputStream, Charset) decodes in place from the pooled buffer; pins it to readAllBytes + decode.
        @Test
        void testReadAllToString_matchesReadAllBytesDecodeAroundBufferAndGrowthBoundaries() {
            final int firstGrowth = (int) (bufferSize * 1.75);
            final int secondGrowth = (int) (firstGrowth * 1.75);
            final int[] lengths = { 0, 1, 2, 3, 100, bufferSize / 2, bufferSize - 2, bufferSize - 1, bufferSize, bufferSize + 1, bufferSize + 2,
                    firstGrowth - 1, firstGrowth, firstGrowth + 1, secondGrowth - 1, secondGrowth, secondGrowth + 1 };
            final int[][] deliveries = { { Integer.MAX_VALUE, 0 }, { 1000, 0 }, { 4099, 10 }, { 1, 0 } };
            long seed = 1;

            for (final int length : lengths) {
                for (int kind = 0; kind < 3; kind++) {
                    final byte[] data = content(kind, length, length * 7L + kind);

                    for (final java.nio.charset.Charset charset : charsets()) {
                        for (final int[] delivery : deliveries) {
                            if (delivery[0] == 1 && length > 300) {
                                continue; // one byte per read only for the small sizes
                            }

                            assertSameAsReference(data, charset, seed++, delivery[0], delivery[1], -1, -1, 0);
                        }
                    }
                }
            }
        }

        // G032-01: zero reads (one retried, two in a row end the read), read failures and contract-breaking counts behave as before.
        @Test
        void testReadAllToString_zeroReadsFailuresAndBrokenCountsMatchReadAllBytes() {
            final Random random = new Random(20261008);
            final List<java.nio.charset.Charset> charsets = charsets();
            final int[] anomalyDeltas = { 1, 3, bufferSize, -2, -5, -100_000 };

            for (int i = 0; i < 1500; i++) {
                final int length = random.nextInt(4) == 0 ? random.nextInt(64) : random.nextInt(3 * bufferSize);
                final byte[] data = content(random.nextInt(3), length, random.nextLong());
                final java.nio.charset.Charset charset = charsets.get(random.nextInt(charsets.size()));
                final int maxChunk = random.nextInt(3) == 0 ? Integer.MAX_VALUE : 1 + random.nextInt(9000);
                final int zeroPercent = random.nextInt(3) == 0 ? 0 : random.nextInt(40);
                final int failAtCall = random.nextInt(5) == 0 ? 1 + random.nextInt(6) : -1;
                final int anomalyAtCall = random.nextInt(5) == 0 ? 1 + random.nextInt(6) : -1;
                final int anomalyDelta = anomalyDeltas[random.nextInt(anomalyDeltas.length)];

                assertSameAsReference(data, charset, random.nextLong(), maxChunk, zeroPercent, failAtCall, anomalyAtCall, anomalyDelta);
            }
        }

        /** A caller-supplied single-byte charset whose decoder records the length of the backing array it is handed. */
        private final class RecordingCharset extends java.nio.charset.Charset {
            final List<Integer> seenArrayLengths = new ArrayList<>();

            RecordingCharset() {
                super("x-g032-recording", null);
            }

            @Override
            public boolean contains(final java.nio.charset.Charset other) {
                return other == this;
            }

            @Override
            public java.nio.charset.CharsetDecoder newDecoder() {
                return new java.nio.charset.CharsetDecoder(this, 1f, 1f) {
                    @Override
                    protected java.nio.charset.CoderResult decodeLoop(final java.nio.ByteBuffer in, final java.nio.CharBuffer out) {
                        if (in.hasArray()) {
                            seenArrayLengths.add(in.array().length);
                        }

                        while (in.hasRemaining()) {
                            if (!out.hasRemaining()) {
                                return java.nio.charset.CoderResult.OVERFLOW;
                            }

                            out.put((char) (in.get() & 0xFF));
                        }

                        return java.nio.charset.CoderResult.UNDERFLOW;
                    }
                };
            }

            @Override
            public java.nio.charset.CharsetEncoder newEncoder() {
                throw new UnsupportedOperationException();
            }
        }

        // G032-01: a caller-supplied Charset is still handed a private exact-size array, never the shared pooled buffer.
        @Test
        void testReadAllToString_callerSuppliedCharsetGetsPrivateExactCopy() {
            for (final int length : new int[] { 1, 1000, bufferSize - 1, bufferSize, bufferSize + 1 }) {
                final byte[] data = content(2, length, length);
                final RecordingCharset expectedCharset = new RecordingCharset();
                final RecordingCharset actualCharset = new RecordingCharset();

                final String expected = reference(new java.io.ByteArrayInputStream(data), expectedCharset);
                final String actual = IOUtil.readAllToString(new java.io.ByteArrayInputStream(data), actualCharset);

                assertEquals(expected, actual);
                assertEquals(expectedCharset.seenArrayLengths, actualCharset.seenArrayLengths);
                assertEquals(Collections.singletonList(length), actualCharset.seenArrayLengths);
            }
        }

        // G032-01: argument validation and the empty source are unchanged.
        @Test
        void testReadAllToString_nullSourceAndEmptySource() {
            final IllegalArgumentException expected = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes((java.io.InputStream) null));
            final IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                    () -> IOUtil.readAllToString((java.io.InputStream) null, java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(expected.getMessage(), actual.getMessage());
            assertEquals(expected.getMessage(),
                    assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllToString((java.io.InputStream) null)).getMessage());

            final String empty = IOUtil.readAllToString(new java.io.ByteArrayInputStream(new byte[0]), null);
            assertEquals("", empty);
            assertNotSame("", empty); // a fresh String, as new String(byte[0], charset) always was

            final byte[] data = content(1, bufferSize - 1, 7);
            final String text = IOUtil.readAllToString(new java.io.ByteArrayInputStream(data));
            // The pooled buffer it was decoded from is wiped and reused by the next read; the result must not change.
            IOUtil.readAllBytes(new java.io.ByteArrayInputStream(content(2, bufferSize - 1, 8)));
            assertEquals(new String(data, java.nio.charset.StandardCharsets.UTF_8), text);
        }
    }

    // ---- perf review 2026-10-08 G032 end ----
    // ---- perf review 2026-10-08 G033 begin ----
    @Nested
    class G033 {
        private java.io.File newFile(final java.io.File dir, final String name, final String content) throws java.io.IOException {
            final java.io.File file = new java.io.File(dir, name);
            java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return file;
        }

        private String read(final java.io.File file) throws java.io.IOException {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        }

        /**
         * The 8.3 short name Windows generated for {@code file}, or {@code null} when the volume generates none (or it
         * cannot be asked).
         */
        private String shortNameOf(final java.io.File file) {
            try {
                final Process process = new ProcessBuilder("cmd", "/c", "for %I in (\"" + file.getAbsolutePath() + "\") do @echo %~sI")
                        .redirectErrorStream(true)
                        .start();
                final String out = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();

                if (process.waitFor() != 0 || out.isEmpty() || out.contains("\n")) {
                    return null;
                }

                final String name = out.substring(out.lastIndexOf('\\') + 1);

                return name.isEmpty() || name.equalsIgnoreCase(file.getName()) ? null : name;
            } catch (final Exception e) {
                return null;
            }
        }

        // G033-01: a file copied into another directory keeps its on-disk name and content, for every source spelling.
        @Test
        void testCopyToDirectory_fileIntoOtherDirectory(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir) throws Exception {
            final java.io.File root = tempDir.toFile().getAbsoluteFile();
            final java.io.File sourceDir = new java.io.File(root, "Source Dir");
            final java.io.File destinationDir = new java.io.File(root, "dest");
            assertTrue(sourceDir.mkdirs());
            final java.io.File source = newFile(sourceDir, "Payload.txt", "hello");

            final java.io.File copy = IOUtil.copyToDirectory(source, destinationDir);

            assertEquals(new java.io.File(destinationDir.getCanonicalFile(), "Payload.txt"), copy);
            assertEquals("Payload.txt", copy.getName());
            assertEquals("hello", read(copy));
            assertEquals(source.lastModified(), copy.lastModified());
            assertEquals("hello", read(source));

            // the same file spelled through "." and ".." elements (and, on Windows, in another case) is copied under its
            // on-disk name, never as "Copy of": the destination is another directory
            final java.io.File otherDir = new java.io.File(root, "other");
            final List<java.io.File> spellings = new ArrayList<>();
            spellings.add(new java.io.File(sourceDir, "./Payload.txt"));
            spellings.add(new java.io.File(new java.io.File(sourceDir, ".."), "Source Dir/Payload.txt"));

            if (IOUtil.IS_OS_WINDOWS) {
                spellings.add(new java.io.File(sourceDir.getPath().toUpperCase(java.util.Locale.ROOT), "PAYLOAD.TXT"));
            }

            for (final java.io.File spelling : spellings) {
                final java.io.File again = IOUtil.copyToDirectory(spelling, otherDir, false);

                assertEquals(new java.io.File(otherDir.getCanonicalFile(), "Payload.txt"), again, spelling.getPath());
                assertEquals("Payload.txt", again.getName(), spelling.getPath());
                assertEquals("hello", read(again), spelling.getPath());
                assertTrue(again.delete());
            }
        }

        // G033-01: a file copied into its own directory is "Copy of <name>" for every spelling of source and destination.
        @Test
        void testCopyToDirectory_fileIntoOwnDirectoryIsCopyOfForEverySpelling(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir)
                throws Exception {
            final java.io.File root = tempDir.toFile().getAbsoluteFile();
            final java.io.File sourceDir = new java.io.File(root, "averyveryverylongdirectorynamewithashortname");
            final java.io.File sub = new java.io.File(sourceDir, "sub");
            assertTrue(sub.mkdirs());
            final java.io.File source = newFile(sourceDir, "Payload.txt", "hello");

            final List<java.io.File[]> pairs = new ArrayList<>();
            pairs.add(new java.io.File[] { source, sourceDir });
            pairs.add(new java.io.File[] { new java.io.File(sourceDir, "./Payload.txt"), sourceDir });
            pairs.add(new java.io.File[] { new java.io.File(sub, "../Payload.txt"), new java.io.File(sub, "..") });
            pairs.add(new java.io.File[] { source, new java.io.File(sourceDir, ".") });
            pairs.add(new java.io.File[] { new java.io.File(new java.io.File(root, "."), sourceDir.getName() + "/Payload.txt"), sub.getParentFile() });

            if (IOUtil.IS_OS_WINDOWS) {
                pairs.add(new java.io.File[] { new java.io.File(sourceDir.getPath().toUpperCase(java.util.Locale.ROOT), "PAYLOAD.TXT"), sourceDir });
                pairs.add(new java.io.File[] { source, new java.io.File(sourceDir.getPath().toLowerCase(java.util.Locale.ROOT)) });

                final String shortName = shortNameOf(sourceDir);

                if (shortName != null) {
                    pairs.add(new java.io.File[] { new java.io.File(new java.io.File(root, shortName), "Payload.txt"), sourceDir });
                    pairs.add(new java.io.File[] { source, new java.io.File(root, shortName) });
                }
            }

            for (final java.io.File[] pair : pairs) {
                final String label = pair[0].getPath() + " -> " + pair[1].getPath();
                final java.io.File copy = IOUtil.copyToDirectory(pair[0], pair[1], false);

                assertEquals("Copy of Payload.txt", copy.getName(), label);
                assertEquals(sourceDir.getCanonicalFile(), copy.getParentFile().getCanonicalFile(), label);
                assertEquals("hello", read(copy), label);
                assertTrue(copy.delete(), label);
            }

            assertEquals("hello", read(source));
            assertEquals(java.util.Arrays.asList("Payload.txt", "sub"), sortedNames(sourceDir));
        }

        // G033-01: a second copy into its own directory is refused ("already exists"); source and first copy untouched.
        @Test
        void testCopyToDirectory_secondCopyIntoOwnDirectoryIsRefused(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir) throws Exception {
            final java.io.File root = tempDir.toFile().getAbsoluteFile();
            final java.io.File source = newFile(root, "doc.txt", "v1");

            final java.io.File first = IOUtil.copyToDirectory(source, root);
            assertEquals("Copy of doc.txt", first.getName());
            java.nio.file.Files.write(first.toPath(), "edited".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            final java.io.IOException failure = assertThrows(java.io.IOException.class, () -> IOUtil.copyToDirectory(source, root));
            assertTrue(failure.getMessage().contains("already exists"), failure.getMessage());
            assertEquals("edited", read(first));
            assertEquals("v1", read(source));
        }

        // G033-01: a directory source is recreated by name with its contents; copying it into itself is refused.
        @Test
        void testCopyToDirectory_directorySource(@org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir) throws Exception {
            final java.io.File root = tempDir.toFile().getAbsoluteFile();
            final java.io.File tree = new java.io.File(root, "tree");
            final java.io.File inner = new java.io.File(tree, "inner");
            assertTrue(inner.mkdirs());
            newFile(tree, "leaf.txt", "leaf");
            newFile(inner, "deep.txt", "deep");
            final java.io.File destinationDir = new java.io.File(root, "dest");

            final java.io.File copy = IOUtil.copyToDirectory(new java.io.File(tree, "."), destinationDir, false);

            assertEquals(new java.io.File(destinationDir.getCanonicalFile(), "tree"), copy);
            assertEquals("leaf", read(new java.io.File(copy, "leaf.txt")));
            assertEquals("deep", read(new java.io.File(copy, "inner/deep.txt")));
            assertEquals(java.util.Arrays.asList("inner", "leaf.txt"), sortedNames(copy));

            assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(tree, inner));
            assertEquals(java.util.Arrays.asList("deep.txt"), sortedNames(inner));
        }

        private List<String> sortedNames(final java.io.File dir) {
            final String[] names = dir.list();
            assertNotNull(names);
            Arrays.sort(names);
            return Arrays.asList(names);
        }
    }

    // ---- perf review 2026-10-08 G033 end ----
    // ---- perf review 2026-10-08 G034 begin ----
    @Nested
    class G034 {

        @org.junit.jupiter.api.io.TempDir
        java.nio.file.Path tempDir;

        private java.io.File dir(final java.io.File parent, final String relative) {
            final java.io.File d = new java.io.File(parent, relative);
            assertTrue(d.isDirectory() || d.mkdirs(), "cannot create " + d);
            return d;
        }

        private java.io.File file(final java.io.File parent, final String relative) throws java.io.IOException {
            final java.io.File f = new java.io.File(parent, relative);
            java.nio.file.Files.write(f.toPath(), relative.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return f;
        }

        /** Files at every depth, empty directories, a chain of directories holding only directories, odd names. */
        private java.io.File mixedTree() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-mixed");
            file(root, "f0.txt");
            file(root, "a b.txt");
            file(root, "noext");
            dir(root, "empty");
            dir(root, "d1/d2");
            dir(root, "d1/d3");
            file(root, "d1/x.txt");
            file(root, "d1/d2/p.txt");
            file(root, "d1/d2/q");
            dir(root, "chain/c1/c2/c3");
            dir(root, "deep/k1");
            file(root, "deep/only.txt");
            file(root, "deep/k1/leaf.txt");
            return root;
        }

        /** Depth-first, pre-order reference of the documented traversal for a tree without links. */
        private void reference(final java.io.File directory, final boolean recursively, final boolean directoriesOnly, final List<java.io.File> out) {
            final java.io.File[] subFiles = directory.listFiles();

            if (subFiles == null) {
                return;
            }

            for (final java.io.File f : subFiles) {
                if (f.isDirectory() == directoriesOnly) {
                    out.add(f);
                }

                if (recursively && f.isDirectory()) {
                    reference(f, recursively, directoriesOnly, out);
                }
            }
        }

        private List<java.io.File> reference(final java.io.File root, final boolean recursively, final boolean directoriesOnly) {
            final List<java.io.File> out = new ArrayList<>();
            reference(root, recursively, directoriesOnly, out);
            return out;
        }

        // G034-01: listFiles(.., true, true) asks isDirectory() once per entry; order, content and traversal unchanged
        @Test
        public void testListFiles_excludeDirectoryRecursiveKeepsPreOrderAndContent() throws java.io.IOException {
            final java.io.File root = mixedTree();
            final List<java.io.File> expected = reference(root, true, false);

            assertEquals(8, expected.size());
            assertEquals(expected, IOUtil.listFiles(root, true, true));
            // the same selection through a caller filter (the general path) and through the lazy twin
            assertEquals(expected, IOUtil.listFiles(root, true, (parent, f) -> !f.isDirectory()));
            assertEquals(expected, IOUtil.walk(root, true, true).toList());

            final List<java.io.File> deepOnly = IOUtil.listFiles(new java.io.File(root, "deep"), true, true);
            assertEquals(2, deepOnly.size());
            assertEquals(reference(new java.io.File(root, "deep"), true, false), deepOnly);
            // a tree holding directories only yields nothing, yet every level is descended into
            assertEquals(new ArrayList<>(), IOUtil.listFiles(new java.io.File(root, "chain"), true, true));
        }

        // G034-01: listDirectories(.., true) asks isDirectory() once per entry; order, content and traversal unchanged
        @Test
        public void testListDirectories_recursiveKeepsPreOrderAndContent() throws java.io.IOException {
            final java.io.File root = mixedTree();
            final List<java.io.File> expected = reference(root, true, true);

            assertEquals(10, expected.size());
            assertEquals(expected, IOUtil.listDirectories(root, true));
            assertEquals(expected, IOUtil.listFiles(root, true, (parent, f) -> f.isDirectory()));
            // every directory of the eager all-entries listing, in the same relative order
            final List<java.io.File> all = IOUtil.listFiles(root, true, false);
            final List<java.io.File> dirsOfAll = new ArrayList<>();

            for (final java.io.File f : all) {
                if (f.isDirectory()) {
                    dirsOfAll.add(f);
                }
            }

            assertEquals(expected, dirsOfAll);
            assertEquals(Arrays.asList(new java.io.File(root, "chain/c1"), new java.io.File(root, "chain/c1/c2"), new java.io.File(root, "chain/c1/c2/c3")),
                    IOUtil.listDirectories(new java.io.File(root, "chain"), true));
        }

        // G034-01: the non-recursive selections by kind and the root edge cases answer exactly as before
        @Test
        public void testListFiles_byKindNonRecursiveAndEdgeRoots() throws java.io.IOException {
            final java.io.File root = mixedTree();

            assertEquals(reference(root, false, false), IOUtil.listFiles(root, false, true));
            assertEquals(3, IOUtil.listFiles(root, false, true).size());
            assertEquals(reference(root, false, true), IOUtil.listDirectories(root));
            assertEquals(reference(root, false, true), IOUtil.listDirectories(root, false));
            assertEquals(4, IOUtil.listDirectories(root).size());

            // fresh, modifiable ArrayList results, also when empty
            final java.io.File empty = new java.io.File(root, "empty");
            final List<java.io.File> r1 = IOUtil.listFiles(empty, true, true);
            final List<java.io.File> r2 = IOUtil.listFiles(empty, true, true);
            assertEquals(ArrayList.class, r1.getClass());
            assertTrue(r1.isEmpty());
            assertNotSame(r1, r2);
            r1.add(empty);
            assertEquals(1, r1.size());
            assertEquals(ArrayList.class, IOUtil.listDirectories(empty, true).getClass());
            assertTrue(IOUtil.listDirectories(empty, true).isEmpty());

            // null and missing roots: empty results; a file root: the wrong-kind IllegalArgumentException
            assertTrue(IOUtil.listFiles(null, true, true).isEmpty());
            assertTrue(IOUtil.listDirectories(null, true).isEmpty());
            final java.io.File missing = new java.io.File(root, "missing");
            assertTrue(IOUtil.listFiles(missing, true, true).isEmpty());
            assertTrue(IOUtil.listDirectories(missing, true).isEmpty());
            final java.io.File aFile = new java.io.File(root, "f0.txt");
            final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> IOUtil.listFiles(aFile, true, true));
            assertEquals("'" + aFile.getAbsolutePath() + "' is not a directory", e1.getMessage());
            final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> IOUtil.listDirectories(aFile, true));
            assertEquals("'" + aFile.getAbsolutePath() + "' is not a directory", e2.getMessage());
            assertThrows(IllegalArgumentException.class,
                    () -> IOUtil.listFiles(root, true, (Throwables.BiPredicate<java.io.File, java.io.File, RuntimeException>) null));
        }

        // G034-01: a linked directory (junction on Windows, symbolic link elsewhere) is selected by kind but never descended into
        @Test
        public void testListFiles_byKindNeverDescendsIntoALinkedDirectory() throws Exception {
            final java.io.File root = dir(tempDir.toFile(), "g034-links");
            final java.io.File target = dir(tempDir.toFile(), "g034-link-target");
            file(target, "inside.txt");
            dir(target, "innerDir");
            file(root, "own.txt");
            dir(root, "sub");
            file(root, "sub/a.txt");

            final java.io.File link = new java.io.File(new java.io.File(root, "sub"), "link");

            if (IOUtil.IS_OS_WINDOWS) {
                final Process p = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.getAbsolutePath(), target.getAbsolutePath()).redirectErrorStream(true)
                        .start();
                p.getInputStream().readAllBytes();
                org.junit.jupiter.api.Assumptions.assumeTrue(
                        p.waitFor() == 0 && java.nio.file.Files.exists(link.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS),
                        "mklink /J is not available here");
            } else {
                try {
                    java.nio.file.Files.createSymbolicLink(link.toPath(), target.toPath());
                } catch (final java.io.IOException | UnsupportedOperationException e) {
                    org.junit.jupiter.api.Assumptions.abort("cannot create a symbolic link here: " + e);
                }
            }

            try {
                assertTrue(link.isDirectory(), "the link resolves to a directory");
                // the order of root's own two entries is the file system's listing order: compared as a set here
                assertEquals(new HashSet<>(Arrays.asList(new java.io.File(root, "own.txt"), new java.io.File(root, "sub/a.txt"))),
                        new HashSet<>(IOUtil.listFiles(root, true, true)));
                assertEquals(2, IOUtil.listFiles(root, true, true).size());
                assertEquals(Arrays.asList(new java.io.File(root, "sub"), link), IOUtil.listDirectories(root, true));
                assertEquals(IOUtil.listFiles(root, true, (parent, f) -> !f.isDirectory()), IOUtil.listFiles(root, true, true));
                assertEquals(IOUtil.listFiles(root, true, (parent, f) -> f.isDirectory()), IOUtil.listDirectories(root, true));
                assertEquals(IOUtil.walk(root, true, true).toList(), IOUtil.listFiles(root, true, true));
                // a linked directory handed in as the root itself is followed and listed
                assertEquals(Arrays.asList(new java.io.File(link, "inside.txt")), IOUtil.listFiles(link, true, true));
                assertEquals(Arrays.asList(new java.io.File(link, "innerDir")), IOUtil.listDirectories(link, true));
            } finally {
                // removes the link entry only (a junction is unlinked by Files.delete, its target is untouched)
                java.nio.file.Files.deleteIfExists(link.toPath());
            }

            assertTrue(new java.io.File(target, "inside.txt").isFile(), "the link target is untouched");
        }

        /** A {@code File} that counts its {@code length()} lookups (a file-system call each). */
        private static final class LengthCountingFile extends java.io.File {
            private static final long serialVersionUID = 1L;

            private int lengthCalls;

            LengthCountingFile(final java.io.File file) {
                super(file.getPath());
            }

            @Override
            public long length() {
                lengthCalls++;
                return super.length();
            }

            int takeLengthCalls() {
                final int result = lengthCalls;
                lengthCalls = 0;
                return result;
            }
        }

        private static final String G034_TEXT = "alpha\nbeta\r\ngamma\n\ndelta";

        private java.io.File gzipOf(final java.io.File target, final String content) throws java.io.IOException {
            try (java.io.OutputStream os = new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(target))) {
                os.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            return target;
        }

        // G034-02: openFile looks the file length up only for a line-count sample; the read family returns the same content
        @Test
        public void testReadFamily_plainGzipAndZipSourcesReadTheSameContent() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-read");
            final java.io.File plain = new java.io.File(root, "plain.txt");
            java.nio.file.Files.write(plain.toPath(), G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            final java.io.File gz = gzipOf(new java.io.File(root, "plain.txt.gz"), G034_TEXT);
            final java.io.File zip = new java.io.File(root, "one.zip");

            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zip))) {
                zos.putNextEntry(new java.util.zip.ZipEntry("d/"));
                zos.closeEntry();
                zos.putNextEntry(new java.util.zip.ZipEntry("d/a.txt"));
                zos.write(G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            final List<String> expectedLines = Arrays.asList("alpha", "beta", "gamma", "", "delta");

            for (final java.io.File source : new java.io.File[] { plain, gz, zip }) {
                final String name = source.getName();
                assertArrayEquals(G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8), IOUtil.readAllBytes(source), name);
                assertEquals(G034_TEXT, IOUtil.readAllToString(source), name);
                assertEquals(G034_TEXT, new String(IOUtil.readAllChars(source)), name);
                assertEquals(expectedLines, IOUtil.readAllLines(source), name);
                assertEquals(Arrays.asList("beta", "gamma"), IOUtil.readLines(source, 1, 2), name);
                assertEquals("alpha", IOUtil.readFirstLine(source), name);
                assertEquals("delta", IOUtil.readLastLine(source), name);
                assertEquals("gamma", IOUtil.readLine(source, 2), name);
                assertArrayEquals(Arrays.copyOfRange(G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8), 1, 8), IOUtil.readBytes(source, 1, 7), name);

                final List<String> lines = new ArrayList<>();
                IOUtil.forEachLine(source, lines::add);
                assertEquals(expectedLines, lines, name);

                final byte[] buf = new byte[64];
                assertEquals(G034_TEXT.length(), IOUtil.read(source, buf), name);
            }

            // an empty plain file: empty results, not end-of-stream errors
            final java.io.File empty = new java.io.File(root, "empty.txt");
            java.nio.file.Files.write(empty.toPath(), new byte[0]);
            assertEquals(0, IOUtil.readAllBytes(empty).length);
            assertTrue(IOUtil.readAllLines(empty).isEmpty());
            assertNull(IOUtil.readFirstLine(empty));
            assertEquals(-1, IOUtil.read(empty, new byte[4]));
        }

        // G034-02: missing, directory, empty-gzip and not-gzip sources still fail with the same exception, cause and message
        @Test
        public void testReadFamily_failedOpensKeepTheirExceptions() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-read-fail");
            final java.io.File missing = new java.io.File(root, "missing.txt");
            final java.io.File missingGz = new java.io.File(root, "missing.gz");
            final java.io.File directory = dir(root, "adir");
            final java.io.File directoryGz = dir(root, "adir.gz");
            final java.io.File emptyGz = new java.io.File(root, "empty.gz");
            java.nio.file.Files.write(emptyGz.toPath(), new byte[0]);
            final java.io.File notGzip = new java.io.File(root, "notgzip.gz");
            java.nio.file.Files.write(notGzip.toPath(), "plain".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            for (final java.io.File absent : new java.io.File[] { missing, missingGz }) {
                final com.landawn.abacus.exception.UncheckedIOException e1 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> IOUtil.readAllBytes(absent));
                assertEquals(java.io.FileNotFoundException.class, e1.getCause().getClass());
                final com.landawn.abacus.exception.UncheckedIOException e2 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> IOUtil.readAllLines(absent));
                assertEquals(java.io.FileNotFoundException.class, e2.getCause().getClass());
                assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.read(absent, new byte[4]));
                final com.landawn.abacus.exception.UncheckedIOException e3 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> IOUtil.forEachLine(absent, line -> {
                        }));
                assertEquals("'" + absent.getAbsolutePath() + "' does not exist", e3.getCause().getMessage());
            }

            for (final java.io.File wrongKind : new java.io.File[] { directory, directoryGz }) {
                final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllBytes(wrongKind));
                assertEquals("'" + wrongKind.getAbsolutePath() + "' is a directory, not a file", e1.getMessage());
                assertEquals(java.io.FileNotFoundException.class, e1.getCause().getClass());
                final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> IOUtil.readAllLines(wrongKind));
                assertEquals("'" + wrongKind.getAbsolutePath() + "' is a directory, not a file", e2.getMessage());
                assertThrows(IllegalArgumentException.class, () -> IOUtil.read(wrongKind, new byte[4]));
            }

            final com.landawn.abacus.exception.UncheckedIOException e4 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                    () -> IOUtil.readAllLines(emptyGz));
            assertEquals(java.io.EOFException.class, e4.getCause().getClass());
            assertThrows(java.io.EOFException.class, () -> IOUtil.read(emptyGz, new byte[4]));

            final com.landawn.abacus.exception.UncheckedIOException e5 = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                    () -> IOUtil.readAllBytes(notGzip));
            assertEquals(java.util.zip.ZipException.class, e5.getCause().getClass());
            assertEquals("Not in GZIP format", e5.getCause().getMessage());
            assertThrows(java.util.zip.ZipException.class, () -> IOUtil.read(notGzip, new byte[4]));
        }

        // G034-02: the sampled line-count estimate of splitByLine still scales the sample by the file length
        @Test
        public void testSplitByLine_sampledEstimateStillScalesByTheFileLength() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-split");
            final java.io.File source = new java.io.File(root, "big.txt");
            final int lineCount = 120_000;
            final StringBuilder sb = new StringBuilder(lineCount * 42);

            for (int i = 0; i < lineCount; i++) {
                sb.append("line ").append(i).append(" of the sampled line-count estimate\n");
            }

            java.nio.file.Files.write(source.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // larger than the 4 MB minimum sample and the 10,000-line sample, so the estimate is scaled, not counted
            assertTrue(source.length() > 4L * 1024 * 1024, "the source must exceed the minimum sample: " + source.length());

            final java.io.File destination = dir(root, "parts");
            IOUtil.splitByLine(source, 4, destination);

            final String[] names = destination.list();
            assertNotNull(names);
            Arrays.sort(names);
            assertEquals(Arrays.asList("big_0001.txt", "big_0002.txt", "big_0003.txt", "big_0004.txt"), Arrays.asList(names));

            long total = 0;
            long biggest = 0;

            for (final String name : names) {
                final long n = IOUtil.readAllLines(new java.io.File(destination, name)).size();
                total += n;
                biggest = Math.max(biggest, n);
            }

            assertEquals(lineCount, total, "no line may be lost");
            // Unscaled (the sample count alone, ~100,000 lines here) would leave ~45,000 lines in the last part.
            assertTrue(biggest <= lineCount / 4 + lineCount / 50, "parts are unbalanced; biggest = " + biggest);
        }

        // G034-02: perf pin - only the presize hint (readAllBytes/readAllToString of a plain file) and the line-count sample
        // look the length up. RED on base by design: there every read looked it up once more, for nothing.
        @Test
        public void testReadFamily_looksTheFileLengthUpOnlyWhereItIsUsed() throws java.io.IOException {
            final java.io.File root = dir(tempDir.toFile(), "g034-length");
            final java.io.File plainFile = new java.io.File(root, "plain.txt");
            java.nio.file.Files.write(plainFile.toPath(), G034_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            final LengthCountingFile plain = new LengthCountingFile(plainFile);
            final LengthCountingFile gz = new LengthCountingFile(gzipOf(new java.io.File(root, "plain.txt.gz"), G034_TEXT));

            for (final LengthCountingFile source : Arrays.asList(plain, gz)) {
                final String name = source.getName();
                source.takeLengthCalls();
                IOUtil.readAllLines(source);
                assertEquals(0, source.takeLengthCalls(), name + " readAllLines");
                IOUtil.readFirstLine(source);
                assertEquals(0, source.takeLengthCalls(), name + " readFirstLine");
                IOUtil.readAllChars(source);
                assertEquals(0, source.takeLengthCalls(), name + " readAllChars");
                IOUtil.read(source, new byte[8]);
                assertEquals(0, source.takeLengthCalls(), name + " read(File, byte[])");
                IOUtil.forEachLine(source, line -> {
                });
                assertEquals(0, source.takeLengthCalls(), name + " forEachLine");
            }

            // the presize hint of the whole-file byte readers is a real use of the length (plain files only)
            IOUtil.readAllBytes(plain);
            assertEquals(1, plain.takeLengthCalls(), "readAllBytes presize hint");
            IOUtil.readAllToString(plain);
            assertEquals(1, plain.takeLengthCalls(), "readAllToString presize hint");
            IOUtil.readAllBytes(gz);
            assertEquals(0, gz.takeLengthCalls(), "a decompressed stream has no length to presize from");
        }
    }
}
