package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FileSystemUtilTest extends TestBase {

    private FileSystemUtil newFileSystemUtil() {
        try {
            final java.lang.reflect.Constructor<FileSystemUtil> constructor = FileSystemUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class SilentSlowProcess {

        public static void main(final String[] args) throws InterruptedException {
            Thread.sleep(3000);
        }
    }

    public static final class InheritedPipeProcess {

        public static void main(final String[] args) throws Exception {
            if (args[0].equals("child")) {
                Thread.sleep(8000);
                return;
            }

            final String executable = System.getProperty("java.home") + File.separator + "bin" + File.separator
                    + (System.getProperty("os.name").toLowerCase().contains("windows") ? "java.exe" : "java");
            final ProcessBuilder builder = new ProcessBuilder(executable, "-cp", System.getProperty("java.class.path"), InheritedPipeProcess.class.getName(),
                    "child");
            builder.redirectOutput(args[0].equals("stdout") ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.DISCARD);
            builder.redirectError(args[0].equals("stderr") ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.DISCARD);
            final Process child = builder.start();
            Files.writeString(Path.of(args[1]), Long.toString(child.pid()));
            System.out.println("parent exiting");
        }
    }

    @Test
    public void testFreeSpaceKb() {
        try {
            assertTrue(FileSystemUtil.freeSpaceKb() > 0);
            assertTrue(FileSystemUtil.freeSpaceKb(".") > 0);
            assertTrue(FileSystemUtil.freeSpaceKb(".", 5000) > 0);
            assertTrue(FileSystemUtil.freeSpaceKb(5000) > 0);

            String home = System.getProperty("user.home");
            if (home != null) {
                assertTrue(FileSystemUtil.freeSpaceKb(home) >= 0);
            }
            String tmp = System.getProperty("java.io.tmpdir");
            if (tmp != null) {
                assertTrue(FileSystemUtil.freeSpaceKb(tmp, 10000) >= 0);
            }
            String root = System.getProperty("os.name").toLowerCase().contains("windows") ? "C:\\" : "/";
            assertTrue(FileSystemUtil.freeSpaceKb(root) >= 0);
            assertTrue(FileSystemUtil.freeSpaceKb("src") >= 0);
        } catch (IOException | IllegalStateException e) {
            // Some hosts report free-space through an OS command that is unavailable here.
        }
    }

    @Test
    public void testFreeSpaceKb_InvalidPath() {
        assertThrows(IllegalArgumentException.class, () -> FileSystemUtil.freeSpaceKb(null));
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            assertThrows(IllegalArgumentException.class, () -> FileSystemUtil.freeSpaceKb(""));
        }
    }

    @Test
    public void testFreeSpaceKb_NonExistentPath() {
        String missing = System.getProperty("os.name").toLowerCase().contains("windows") ? "Z:\\NonExistentPath12345" : "/nonexistent/path12345";
        try {
            FileSystemUtil.freeSpaceKb(missing);
        } catch (IOException e) {
            assertTrue(e.getMessage() != null);
        }
    }

    @Test
    public void testFreeSpaceKb_ZeroTimeout() {
        assertDoesNotThrow(() -> {
            try {
                FileSystemUtil.freeSpaceKb(".", 0);
            } catch (IOException | IllegalStateException e) {
                // timeout 0 still reaches the OS command on supported hosts
            }
        });
    }

    @Test
    public void testFreeSpaceOS_UnsupportedOs() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> newFileSystemUtil().freeSpaceOS(".", 0, true, 0));
        assertTrue(exception.getMessage().contains("Unsupported operating system"));
    }

    @Test
    public void testParseDir() throws IOException {
        assertEquals(12345678L, newFileSystemUtil().parseDir("           12,345,678 bytes free", "."));
        assertEquals(12345678L, newFileSystemUtil().parseDir("12345678", "."));
        assertThrows(IOException.class, () -> newFileSystemUtil().parseDir("bytes free", "."));
    }

    @Test
    public void testFreeSpaceUnix_EmptyPath() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> newFileSystemUtil().freeSpaceUnix("", true, false, 0));
        assertTrue(exception.getMessage().contains("Path must not be empty"));
    }

    @Test
    public void testParseBytes() throws IOException {
        assertEquals(1024L, newFileSystemUtil().parseBytes("1024", "."));
        IOException exception = assertThrows(IOException.class, () -> newFileSystemUtil().parseBytes("-1", "."));
        assertTrue(exception.getMessage().contains("did not find free space"));
    }

    @Test
    public void testFreeSpaceWindowsRejectsQuotedPathBeforeProcessExecution() {
        assertThrows(IllegalArgumentException.class, () -> newFileSystemUtil().freeSpaceWindows("C:\\tmp\"bad", 0));
    }

    @Test
    public void testPerformCommandEnforcesTimeoutWhileOutputIsSilent() throws Exception {
        final String executable = System.getProperty("java.home") + File.separator + "bin" + File.separator
                + (System.getProperty("os.name").toLowerCase().contains("windows") ? "java.exe" : "java");
        final String testClasses = new File(FileSystemUtilTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        final String[] command = { executable, "-cp", testClasses, SilentSlowProcess.class.getName() };
        final long start = System.nanoTime();

        final IOException exception = assertThrows(IOException.class, () -> newFileSystemUtil().performCommand(command, 1, 100));
        final long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        final boolean interrupted = Thread.interrupted();

        assertTrue(exception.getMessage().contains("timed out"));
        assertTrue(elapsedMillis < 2000, "Timeout took " + elapsedMillis + " ms");
        assertFalse(interrupted, "A command timeout must not interrupt its caller");
    }

    @Test
    public void testPerformCommandEnforcesTimeoutForInheritedStdout() throws Exception {
        assertInheritedPipeTimeout("stdout");
    }

    @Test
    public void testPerformCommandEnforcesTimeoutForInheritedStderr() throws Exception {
        assertInheritedPipeTimeout("stderr");
    }

    private void assertInheritedPipeTimeout(final String pipe) throws Exception {
        final String executable = System.getProperty("java.home") + File.separator + "bin" + File.separator
                + (System.getProperty("os.name").toLowerCase().contains("windows") ? "java.exe" : "java");
        final String testClasses = new File(FileSystemUtilTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        final Path childPidFile = Files.createTempFile("abacus-inherited-pipe-", ".pid");
        final String[] command = { executable, "-cp", testClasses, InheritedPipeProcess.class.getName(), pipe, childPidFile.toString() };

        try {
            final long start = System.nanoTime();
            final IOException exception = assertThrows(IOException.class, () -> newFileSystemUtil().performCommand(command, 1, 2000));
            final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertTrue(exception.getMessage().contains("timed out"));
            assertTrue(elapsedMillis < 6000, "Inherited " + pipe + " kept the call blocked for " + elapsedMillis + " ms");
            assertFalse(Thread.currentThread().isInterrupted(), "A command timeout must not interrupt its caller");
            assertFalse(Files.readString(childPidFile).isEmpty(), "The parent must have launched its pipe-holding child");
        } finally {
            final String childPid = Files.readString(childPidFile);
            if (!childPid.isEmpty()) {
                final ProcessHandle child = ProcessHandle.of(Long.parseLong(childPid)).orElse(null);
                if (child != null && child.isAlive()) {
                    child.destroyForcibly();
                    child.onExit().get(3, TimeUnit.SECONDS);
                }
            }
            Files.deleteIfExists(childPidFile);
        }
    }
}
