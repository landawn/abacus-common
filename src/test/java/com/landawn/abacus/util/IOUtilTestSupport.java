package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.provider.Arguments;

import com.landawn.abacus.TestBase;

public abstract class IOUtilTestSupport extends TestBase {

    @TempDir
    Path tempFolder;

    protected File tempFile;
    protected File largeFile;
    protected File emptyFile;
    protected static final String TEST_CONTENT = "Hello World!";
    protected static final String MULTILINE_CONTENT = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\n";
    protected static final String UNICODE_CONTENT = "Hello 世界 \uD83D\uDE00 Здравствуй мир";
    protected static final Charset UTF_8 = StandardCharsets.UTF_8;
    protected static final Charset UTF_16 = StandardCharsets.UTF_16;
    protected static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

    protected static final String SYMLINK_UNSUPPORTED = "This platform cannot create symbolic links (e.g. Windows without the 'Create symbolic links' privilege).";

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        emptyFile = Files.createTempFile(tempFolder, "empty", ".txt").toFile();
        largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();

        Files.write(tempFile.toPath(), TEST_CONTENT.getBytes(UTF_8));

        StringBuilder largeSb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeSb.append("Line ").append(i).append(": This is a test line with some content.\n");
        }
        Files.write(largeFile.toPath(), largeSb.toString().getBytes(UTF_8));
    }

    @AfterEach
    public void tearDown() {
    }

    protected void checkSplitDoesNotOverwriteSourceThroughHardLink(final int mode) throws IOException {
        final Path source = tempFolder.resolve("split-source-" + mode + ".txt");
        final String content = "first\nsecond\nthird\nfourth\n";
        Files.writeString(source, content);
        final Path part = tempFolder.resolve(mode == 2 ? "split-source-" + mode + "_0001.txt" : source.getFileName() + "_0001");

        try {
            Files.createLink(part, source);
        } catch (final IOException | UnsupportedOperationException e) {
            assumeTrue(false, "Hard links are not supported: " + e.getMessage());
        }

        assertThrows(IllegalArgumentException.class, () -> {
            if (mode == 0) {
                IOUtil.split(source.toFile(), 2, tempFolder.toFile());
            } else if (mode == 1) {
                IOUtil.splitBySize(source.toFile(), 5, tempFolder.toFile());
            } else {
                IOUtil.splitByLine(source.toFile(), 2, tempFolder.toFile(), StandardCharsets.UTF_8);
            }
        });
        assertEquals(content, Files.readString(source));
    }

    /** Try to create a symbolic link; return false if not supported (Windows w/o privilege). */
    protected static boolean trySymlink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (UnsupportedOperationException | IOException e) {
            return false;
        }
    }

    protected interface WriterAction {
        public void run(Writer w) throws IOException;
    }

    protected static String writeToString(final WriterAction action) throws IOException {
        final java.io.StringWriter sw = new java.io.StringWriter();
        action.run(sw);
        return sw.toString();
    }

    static Stream<Arguments> simplifyPathCases() {
        return Stream.of(Arguments.of("/foo/bar/baz", "/foo/bar/baz"), Arguments.of("/foo/./bar", "/foo/bar"), Arguments.of("/foo/bar/../baz", "/foo/baz"),
                Arguments.of("/foo/bar/", "/foo/bar"), Arguments.of("/", "/"), Arguments.of(".", "."), Arguments.of("foo/bar/baz", "foo/bar/baz"),
                Arguments.of("foo/./bar", "foo/bar"), Arguments.of("foo/bar/../baz", "foo/baz"), Arguments.of("..", ".."), Arguments.of("/../foo", "/foo"),
                Arguments.of("C:\\foo\\bar\\..\\baz", "C:/foo/baz"), Arguments.of("/foo\\bar/baz", "/foo/bar/baz"), Arguments.of("/a/./b/../../c/", "/c"),
                Arguments.of("/foo/bar/../../baz", "/baz"), Arguments.of("/foo//bar///baz", "/foo/bar/baz"), Arguments.of("", "."));
    }

    /**
     * Whether a live memory mapping blocks deletion here (Windows) or not (POSIX), asked with a throwaway file.
     */
    protected static boolean aLiveMappingLocksFiles() throws IOException {
        final File canary = File.createTempFile("c003-canary", ".bin");
        IOUtil.write("x", canary);
        final MappedByteBuffer buffer = IOUtil.map(canary);
        assertNotNull(buffer);

        try {
            Files.deleteIfExists(canary.toPath());
            return false;
        } catch (final IOException e) {
            return true;
        } finally {
            unmap(buffer);
            canary.delete();
        }
    }

    protected static InputStream zeroBulkInputStream(final byte[] bytes) {
        return new InputStream() {
            protected int position;

            @Override
            public int read(final byte[] buffer, final int offset, final int length) {
                return length == 0 ? 0 : (position < bytes.length ? 0 : -1);
            }

            @Override
            public int read() {
                return position < bytes.length ? bytes[position++] & 0xff : -1;
            }
        };
    }

    protected static Reader zeroBulkReader(final char[] chars) {
        return new Reader() {
            protected int position;

            @Override
            public int read(final char[] buffer, final int offset, final int length) {
                return length == 0 ? 0 : (position < chars.length ? 0 : -1);
            }

            @Override
            public int read() {
                return position < chars.length ? chars[position++] : -1;
            }

            @Override
            public void close() {
            }
        };
    }

    protected static final class ListingFailureFile extends File {
        protected static final long serialVersionUID = 1L;

        ListingFailureFile(final File file) {
            super(file.getPath());
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean isFile() {
            return false;
        }

        @Override
        public boolean isDirectory() {
            return true;
        }

        @Override
        public File[] listFiles() {
            return null;
        }
    }

    protected static final class ZeroThenEofInputStream extends InputStream {
        protected int arrayReads = 0;

        @Override
        public int read(final byte[] b, final int off, final int len) {
            arrayReads++;

            if (arrayReads == 1) {
                return 0;
            } else if (arrayReads == 2) {
                return -1;
            }

            throw new AssertionError("zero-progress InputStream was read again");
        }

        @Override
        public int read() {
            return -1;
        }
    }

    protected static final class ZeroThenEofReader extends Reader {
        protected int arrayReads = 0;

        @Override
        public int read(final char[] cbuf, final int off, final int len) {
            arrayReads++;

            if (arrayReads == 1) {
                return 0;
            } else if (arrayReads == 2) {
                return -1;
            }

            throw new AssertionError("zero-progress Reader was read again");
        }

        @Override
        public void close() {
        }
    }

    protected static final class CountingOutputStream extends OutputStream {
        protected final AtomicInteger flushCount = new AtomicInteger();

        @Override
        public void write(final int b) {
        }

        @Override
        public void flush() {
            flushCount.incrementAndGet();
        }
    }

    protected static final class CountingWriter extends Writer {
        protected final AtomicInteger flushCount = new AtomicInteger();

        @Override
        public void write(final char[] buf, final int off, final int len) {
        }

        @Override
        public void flush() {
            flushCount.incrementAndGet();
        }

        @Override
        public void close() {
        }
    }
}
