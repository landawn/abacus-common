package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

public class StreamTextConstructionTest extends TestBase {
    @Test
    public void supportedReadersPreserveCharactersAndDeclaredClass() throws Exception {
        for (Class<?> cls : new Class<?>[] { Reader.class, StringReader.class, CharArrayReader.class, BufferedReader.class, PushbackReader.class,
                FilterReader.class }) {
            Type<Reader> type = TypeFactory.getType(cls);
            assertNull(type.valueOf((String) null));
            for (String content : new String[] { "", "ordinary text", "\u0000\uFFFF\uD800x\uDC00\uD83D\uDE00", "\r\n\r\n" }) {
                try (Reader value = type.valueOf(content)) {
                    assertTrue(cls.isInstance(value), cls.getName());
                    assertEquals(content, type.stringOf(value));
                }
            }
        }
    }

    @Test
    public void supportedStreamsPreserveUtf8AndDeclaredClass() throws Exception {
        for (Class<?> cls : new Class<?>[] { InputStream.class, ByteArrayInputStream.class, BufferedInputStream.class, DataInputStream.class,
                PushbackInputStream.class, FilterInputStream.class }) {
            Type<InputStream> type = TypeFactory.getType(cls);
            assertNull(type.valueOf((String) null));
            for (String content : new String[] { "", "ordinary text", "\u0000\uFFFF\uD83D\uDE00\u00E9", "\r\n\r\n" }) {
                try (InputStream value = type.valueOf(content)) {
                    assertTrue(cls.isInstance(value), cls.getName());
                    assertEquals(content, type.stringOf(value));
                }
            }
        }
    }

    @Test
    public void fileDescriptorsCannotOpenTextAsAPath(@TempDir Path directory) throws Exception {
        Path marker = directory.resolve("reader-marker.txt");
        Files.writeString(marker, "private marker");
        for (Class<?> cls : new Class<?>[] { FileReader.class, FileInputStream.class }) {
            Type<?> type = TypeFactory.getType(cls);
            assertNull(type.valueOf((String) null));
            for (String text : new String[] { "", "ordinary text", marker.toString() }) {
                assertThrows(UnsupportedOperationException.class, () -> type.valueOf(text));
            }
        }
        try (FileReader value = new FileReader(marker.toFile())) {
            assertEquals("private marker", TypeFactory.<Reader> getType(FileReader.class).stringOf(value));
        }
    }

    @Test
    public void arbitraryConstructorsAreNotInferredAsContentFactories() {
        CustomReader.called = false;
        CustomStream.called = false;
        for (Class<?> cls : new Class<?>[] { CustomReader.class, CustomStream.class, GZIPInputStream.class }) {
            Type<?> type = TypeFactory.getType(cls);
            assertNull(type.valueOf((String) null));
            assertThrows(UnsupportedOperationException.class, () -> type.valueOf("content"));
        }
        assertEquals(false, CustomReader.called);
        assertEquals(false, CustomStream.called);
    }

    public static class CustomReader extends StringReader {
        static boolean called;

        public CustomReader(String text) {
            super(text);
            called = true;
        }
    }

    public static class CustomStream extends ByteArrayInputStream {
        static boolean called;

        public CustomStream(byte[] bytes) {
            super(bytes);
            called = true;
        }
    }
}
