package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

public class IOUtilGetTest extends IOUtilTestSupport {

    @Test
    public void testGetHostName() {
        String hostName = IOUtil.getHostName();
        assertNotNull(hostName);
        assertFalse(hostName.isEmpty());

        // A resolved name is cached for the lifetime of the JVM, so repeated calls must agree. A failure is
        // deliberately NOT cached permanently, so "UNKNOWN_HOST_NAME" is not asserted to repeat.
        if (!"UNKNOWN_HOST_NAME".equals(hostName)) {
            assertEquals(hostName, IOUtil.getHostName());
        }
    }

    @Test
    public void testGetHostName_ResolverThreadIsDaemon() {
        IOUtil.getHostName();

        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().startsWith("abacus-hostname-resolver")) {
                assertTrue(thread.isDaemon(), "the host-name resolver thread must be a daemon");
            }
        }
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("txt", IOUtil.getFileExtension(new File("test.txt")));
        assertEquals("pdf", IOUtil.getFileExtension("document.pdf"));
        assertEquals("java", IOUtil.getFileExtension("/path/to/file.java"));
        assertEquals("gz", IOUtil.getFileExtension("archive.tar.gz"));
        assertEquals("gitignore", IOUtil.getFileExtension(".gitignore"));
        assertEquals("", IOUtil.getFileExtension("README"));
        assertEquals("", IOUtil.getFileExtension("file."));
    }

    @Test
    public void testGetFileExtension_Null() {
        assertNull(IOUtil.getFileExtension((File) null));
        assertNull(IOUtil.getFileExtension((String) null));
    }

    @Test
    public void testGetFileExtension_Empty() {
        assertEquals("", IOUtil.getFileExtension(""));
    }

    @Test
    public void testGetNameWithoutExtension() {
        assertEquals("document", IOUtil.getNameWithoutExtension(new File("document.txt")));
        assertEquals("test", IOUtil.getNameWithoutExtension(new File("/tmp/test.log")));
        assertEquals("image", IOUtil.getNameWithoutExtension("image.png"));
        assertEquals("LICENSE", IOUtil.getNameWithoutExtension("LICENSE"));
        assertEquals("", IOUtil.getNameWithoutExtension(".hidden"));
        assertEquals("/usr/local/bin/script", IOUtil.getNameWithoutExtension("/usr/local/bin/script.sh"));
        assertEquals("name", IOUtil.getNameWithoutExtension("name."));
        assertEquals("backup.tar", IOUtil.getNameWithoutExtension("backup.tar.gz"));
    }

    @Test
    public void testGetNameWithoutExtension_Null() {
        assertNull(IOUtil.getNameWithoutExtension((File) null));
        assertNull(IOUtil.getNameWithoutExtension((String) null));
    }

    @Test
    public void testGetNameWithoutExtension_Empty() {
        assertEquals("", IOUtil.getNameWithoutExtension(""));
    }

    @Test
    public void testGetFileExtensionAndNameWithoutExtension_NulByteInTheName() {
        final String nameWithNul = new String(new char[] { 'a', (char) 0, 'b', '.', 't', 'x', 't' });
        final File fileWithNul = new File(nameWithNul);

        assertThrows(IllegalArgumentException.class, () -> IOUtil.getFileExtension(nameWithNul));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.getFileExtension(fileWithNul));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.getNameWithoutExtension(nameWithNul));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.getNameWithoutExtension(fileWithNul));
    }
}
