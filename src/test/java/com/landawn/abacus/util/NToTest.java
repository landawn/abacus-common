package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.XmlSerConfig;

public class NToTest extends NTestSupport {

    @Test
    public void testToJson() throws IOException {
        TestBean bean = createSampleBean();
        assertEquals(getExpectedJsonForSampleBean(false), N.toJson(bean));

        String pretty = N.toJson(bean, true);
        assertTrue(pretty.contains("\"name\": \"testName\""));
        assertTrue(pretty.contains("\n"));
        assertEquals(getExpectedJsonForSampleBean(true), N.toJson(bean, JsonSerConfig.create().setPrettyFormat(true)));

        File outputFile = new File(tempDir, "output.json");
        N.toJson(bean, outputFile);
        assertEquals(getExpectedJsonForSampleBean(false), new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8));

        File prettyFile = new File(tempDir, "output_pretty.json");
        N.toJson(bean, JsonSerConfig.create().setPrettyFormat(true), prettyFile);
        assertEquals(getExpectedJsonForSampleBean(true), new String(Files.readAllBytes(prettyFile.toPath()), StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        N.toJson(bean, baos);
        assertEquals(getExpectedJsonForSampleBean(false), baos.toString(StandardCharsets.UTF_8.name()));

        baos.reset();
        N.toJson(bean, JsonSerConfig.create().setPrettyFormat(true), baos);
        assertEquals(getExpectedJsonForSampleBean(true), baos.toString(StandardCharsets.UTF_8.name()));

        StringWriter writer = new StringWriter();
        N.toJson(bean, writer);
        assertEquals(getExpectedJsonForSampleBean(false), writer.toString());

        writer = new StringWriter();
        N.toJson(bean, JsonSerConfig.create().setPrettyFormat(true), writer);
        assertEquals(getExpectedJsonForSampleBean(true), writer.toString());
    }

    @Test
    public void testToXml(@TempDir Path xmlTempDir) throws IOException {
        TestBean bean = createSampleBean();
        String xml = N.toXml(bean);
        assertTrue(xml.contains("<testBean>"));
        assertTrue(xml.contains("<name>testName</name>"));
        assertTrue(xml.contains("<value>123</value>"));

        String pretty = N.toXml(bean, true);
        assertTrue(pretty.contains("<testBean>"));
        assertTrue(pretty.contains("<name>testName</name>"));

        File outputFile = xmlTempDir.resolve("output.xml").toFile();
        N.toXml(bean, outputFile);
        assertTrue(new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8).contains("testName"));

        File prettyFile = xmlTempDir.resolve("output2.xml").toFile();
        N.toXml(bean, XmlSerConfig.create().setPrettyFormat(true), prettyFile);
        assertTrue(new String(Files.readAllBytes(prettyFile.toPath()), StandardCharsets.UTF_8).contains("testName"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        N.toXml(bean, baos);
        assertTrue(baos.toString().contains("testName"));

        StringWriter writer = new StringWriter();
        N.toXml(bean, writer);
        assertTrue(writer.toString().contains("testName"));

        writer = new StringWriter();
        N.toXml(bean, XmlSerConfig.create().setPrettyFormat(true), writer);
        assertTrue(writer.toString().contains("<testBean>"));
    }

    @Test
    public void testToRuntimeException() {
        Exception checked = new IOException("checked");
        RuntimeException wrapped = N.toRuntimeException(checked);
        assertEquals(checked, wrapped.getCause());

        RuntimeException existing = new IllegalArgumentException("runtime");
        assertSame(existing, N.toRuntimeException(existing));

        Error error = new Error("boom");
        assertEquals(error, N.toRuntimeException(error).getCause());
        assertEquals(error, N.toRuntimeException(error, false).getCause());
        assertSame(existing, N.toRuntimeException((Throwable) existing, false));
        assertNotNull(N.toRuntimeException(checked, false));

        assertThrows(OutOfMemoryError.class, () -> ExceptionUtil.toRuntimeException(new OutOfMemoryError("error"), true, true));

        assertFalse(Thread.currentThread().isInterrupted());
        try {
            assertNotNull(N.toRuntimeException((Throwable) new InterruptedException("intr"), true));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testToString() {
        assertEquals("true", CommonUtil.toString(true));
        assertEquals("1", CommonUtil.toString('1'));
        assertEquals("1", CommonUtil.toString((byte) 1));
        assertEquals("1", CommonUtil.toString((short) 1));
        assertEquals("1", CommonUtil.toString(1));
        assertEquals("1", CommonUtil.toString(1L));
        assertEquals("1.0", CommonUtil.toString(1f));
        assertEquals("1.0", CommonUtil.toString(1d));
        assertEquals("[a, b]", CommonUtil.toString(new String[] { "a", "b" }));
        assertEquals("[a, b]", CommonUtil.deepToString(new String[] { "a", "b" }));
        assertEquals(Strings.NULL, CommonUtil.toString((int[]) null));
        assertEquals(Strings.NULL, CommonUtil.toString((int[][]) null));
        assertEquals("[]", CommonUtil.toString(new int[0]));
        assertEquals("[]", CommonUtil.toString(new int[0][]));
        assertEquals("[0]", CommonUtil.toString(new int[1]));
        assertEquals("[a, b]", CommonUtil.toString((Object) new String[] { "a", "b" }));
        assertEquals(Strings.NULL, CommonUtil.deepToString((Object) null));

        assertEquals("[[false, true], [a, b], [1, 2], [1, 2], [1, 2], [1, 2], [1.0, 2.0], [1.0, 2.0], [a, bc]]",
                CommonUtil.deepToString(new Object[] { new boolean[] { false, true }, new char[] { 'a', 'b' }, new byte[] { 1, 2 }, new short[] { 1, 2 },
                        new int[] { 1, 2 }, new long[] { 1, 2 }, new float[] { 1, 2 }, new double[] { 1, 2 }, new String[] { "a", "bc" } }));

        assertEquals("[[1.0, 1.0], [1.2111, 2.111]]", CommonUtil.deepToString(new double[][] { { 1, 1 }, { 1.2111, 2.111 } }));
    }

    @Test
    public void testToListAndSet() {
        assertEquals(Arrays.asList(1, 2, 3), CommonUtil.toList(new Integer[] { 1, 2, 3 }));
        assertEquals(Arrays.asList(2, 3, 4), CommonUtil.toList(new Integer[] { 1, 2, 3, 4, 5 }, 1, 4));
        assertEquals(Collections.emptyList(), CommonUtil.toList(new Integer[] {}));
        assertEquals(Collections.emptyList(), CommonUtil.toList((Integer[]) null));

        Set<Integer> set = CommonUtil.toSet(new Integer[] { 1, 2, 3, 2, 1 });
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertEquals(Collections.emptySet(), CommonUtil.toSet(new Integer[] {}));
        assertEquals(Collections.emptySet(), CommonUtil.toSet((Integer[]) null));
    }
}
