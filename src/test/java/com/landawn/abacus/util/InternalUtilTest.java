package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for {@link InternalUtil} — its members are deprecated/internal, but the helpers are
 * still exercised across the framework, so we cover the contracts here.
 */
public class InternalUtilTest extends TestBase {

    @Test
    public void testErrorMessages() {
        assertNotNull(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        assertTrue(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX.contains("does not exist"));

        assertNotNull(InternalUtil.ERROR_MSG_FOR_NULL_ELEMENT_EX);
        assertTrue(InternalUtil.ERROR_MSG_FOR_NULL_ELEMENT_EX.contains("null"));
    }

    @Test
    public void testPoolSizeWithinDocumentedBounds() {
        // Documented range: [1000, 8192]
        assertTrue(InternalUtil.POOL_SIZE >= 1000, "POOL_SIZE must be >= 1000");
        assertTrue(InternalUtil.POOL_SIZE <= 8192, "POOL_SIZE must be <= 8192");
    }

    @Test
    public void testGetCharsForReadOnly_NormalString() {
        char[] chars = InternalUtil.getCharsForReadOnly("Hello");
        assertEquals(5, chars.length);
        assertEquals('H', chars[0]);
        assertEquals('o', chars[4]);
    }

    @Test
    public void testGetCharsForReadOnly_EmptyString() {
        char[] chars = InternalUtil.getCharsForReadOnly("");
        assertSame(CommonUtil.EMPTY_CHAR_ARRAY, chars);
    }

    @Test
    public void testGetCharsForReadOnly_NullString() {
        char[] chars = InternalUtil.getCharsForReadOnly(null);
        assertSame(CommonUtil.EMPTY_CHAR_ARRAY, chars);
    }

    @Test
    public void testGetCharsForReadOnly_UnicodeString() {
        char[] chars = InternalUtil.getCharsForReadOnly("éà");
        assertEquals(2, chars.length);
        assertEquals('é', chars[0]);
    }

    /**
     * The static initializer must leave {@code isListElementDataFieldGettable} agreeing with whether
     * {@code ArrayList.elementData} is actually readable.
     *
     * <p>This JVM can only ever exercise one of the two branches: the Surefire {@code argLine} in {@code pom.xml}
     * opens {@code java.lang}, {@code java.lang.reflect}, {@code java.io}, {@code java.nio} and
     * {@code sun.nio.ch}, but NOT {@code java.util}, so {@code setAccessible} is refused here and the flag is
     * legitimately {@code false} - the JDK 16 default. Both branches are therefore covered by launching a child
     * JVM twice, once without {@code --add-opens java.base/java.util} and once with it.
     */
    @Test
    public void testListElementDataFieldFlagAgreesWithActualAccessibility() throws Exception {
        Path dir = Files.createTempDirectory("abacus-internalutil-probe");
        Path src = dir.resolve("InternalUtilFlagProbe.java");
        Files.writeString(src, String.join("\n", //
                "import java.lang.reflect.Field;", //
                "public class InternalUtilFlagProbe {", //
                "    public static void main(String[] args) throws Exception {", //
                "        Class<?> c = Class.forName(\"com.landawn.abacus.util.InternalUtil\");", //
                "        Field flag = c.getDeclaredField(\"isListElementDataFieldGettable\");", //
                "        flag.setAccessible(true);", //
                "        boolean valueBeforeAnyCall = flag.getBoolean(null);", //
                "        boolean readable;", //
                "        try {", //
                "            java.util.ArrayList.class.getDeclaredField(\"elementData\").setAccessible(true);", //
                "            readable = true;", //
                "        } catch (Throwable t) {", //
                "            readable = false;", //
                "        }", //
                "        System.out.println(\"READABLE=\" + readable + \" FLAG=\" + valueBeforeAnyCall);", //
                "    }", //
                "}"), StandardCharsets.UTF_8);

        try {
            assertEquals("READABLE=false FLAG=false", runFlagProbe(dir, src),
                    "setAccessible was refused, so the static initializer must record the field as unreadable");
            assertEquals("READABLE=true FLAG=true", runFlagProbe(dir, src, "--add-opens", "java.base/java.util=ALL-UNNAMED"),
                    "setAccessible succeeds, so the static initializer must record the field as readable");
        } finally {
            Files.deleteIfExists(src);
            Files.deleteIfExists(dir);
        }
    }

    private static String runFlagProbe(final Path dir, final Path src, final String... vmArgs) throws Exception {
        String javaExe = ProcessHandle.current().info().command().orElse(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        // Run against exactly the classes this test is running against, keeping the child command line short.
        String classpath = new File(InternalUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();

        List<String> command = new ArrayList<>();
        command.add(javaExe);
        command.addAll(List.of(vmArgs));
        command.addAll(List.of("-cp", classpath, src.toString()));

        // Redirect to a file rather than read the pipe: waitFor has to come first for its timeout to bound a hung
        // child (with redirectErrorStream the child's stdout closes only when it exits, so readAllBytes() would
        // block forever and the timeout would never fire), and waiting first on a pipe risks the opposite
        // deadlock - a child blocked writing into a full buffer never exits.
        Path out = dir.resolve("probe-out.txt");
        Process proc = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(Redirect.to(out.toFile())).start();
        String output;
        try {
            assertTrue(proc.waitFor(120, TimeUnit.SECONDS), "probe JVM did not terminate");
            output = Files.readString(out, StandardCharsets.UTF_8);
        } finally {
            proc.destroyForcibly();
            Files.deleteIfExists(out);
        }

        String result = output.lines().filter(line -> line.startsWith("READABLE=")).findFirst().orElse(null);
        assertNotNull(result, "probe JVM produced no result. Output was:\n" + output);

        return result;
    }
}
