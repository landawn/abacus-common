package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.management.ManagementFactory;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.PatternSyntaxException;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;

@Tag("unit")
public class ParserUtilMetadataPerformance20261030Test {
    private static volatile Object observed;

    @Test
    public void tolerantAliasesKeepTheirRankingCaseRulesAndUnicodeMatching() {
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(AliasBean.class);
        final ParserUtil.PropInfo value = info.getPropInfo("value");
        for (final String spelling : new String[] { "getValue", "setVALUE", "isValue", "hasValue", "AliasBean.VaLuE", "  getValue  ", "v_a_l_u_e" }) {
            assertSame(value, info.getPropInfo(spelling), spelling);
        }
        assertNull(info.getPropInfo("GETvalue"));
        assertNull(info.getPropInfo("get"));
        assertNull(info.getPropInfo("is"));
        assertNull(info.getPropInfo("getValueExtra"));
        assertNull(info.getPropInfo("AliasBean.valueExtra"));
        assertSame(info.getPropInfo("k"), info.getPropInfo("get\u212A"));
        assertSame(info.getPropInfo("i"), info.getPropInfo("AliasBean.\u0130"));
        assertSame(info.getPropInfo("\uD801\uDC28"), info.getPropInfo("get\uD801\uDC00"));
        assertSame(info.getPropInfo("hasChildren"), info.getPropInfo("haschildren"));
    }

    @Test
    public void qualifiedAliasStillYieldsToAValidNestedPath() {
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Node.class);
        assertNull(info.getPropInfo("node.value"));
        assertEquals(2, info.getPropInfoChain("node.value").size());
        final Node outer = new Node();
        outer.value = "outer";
        outer.node = new AliasBean();
        outer.node.value = "inner";
        assertEquals("inner", info.getPropValue(outer, "node.value"));
    }

    @Test
    public void ignoredPatternsPreserveLiteralRegexComputedAndBackingFieldBehavior() throws Exception {
        final JsonParser parser = ParserFactory.createJsonParser();
        final PatternBean bean = new PatternBean();
        final String json = parser.serialize(bean);
        assertTrue(json.contains("visible"));
        assertFalse(json.contains("hidden"));
        assertFalse(json.contains("secret"));
        assertFalse(json.contains("computed"));
        final PatternBean restored = parser.deserialize("{\"hiddenValue\":17,\"visible\":9}", PatternBean.class);
        assertEquals(17, restored.hiddenValue, "ignoredFields affects serialization, not assignment");
        assertEquals(9, restored.visible);
        assertFalse(
                ParserUtil.isJsonXmlSerializable("renamed", PatternBean.class.getField("hiddenValue"), PatternBean.class.getAnnotation(JsonXmlConfig.class)));
        assertTrue(ParserUtil.isJsonXmlSerializable(null, null, PatternBean.class.getAnnotation(JsonXmlConfig.class)));
    }

    @Test
    public void invalidRegexIsOnlyValidatedWhenExistingShortCircuitsReachIt() {
        assertNotNull(ParserUtil.getBeanInfo(ExactBeforeInvalid.class));
        assertNotNull(ParserUtil.getBeanInfo(FieldIgnoreBeforeInvalid.class));
        assertThrows(PatternSyntaxException.class, () -> ParserUtil.getBeanInfo(InvalidBeforeExact.class));
        assertThrows(PatternSyntaxException.class, () -> ParserUtil.getBeanInfo(UnmatchedInvalid.class));
    }

    @Test
    @Tag("slow-test")
    public void uncachedPrefixLookupAvoidsPerCandidateTemporaryStrings(@TempDir final Path directory) throws Exception {
        final com.sun.management.ThreadMXBean allocations = allocations();
        try (URLClassLoader loader = compileWideBean(directory, false)) {
            final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Class.forName("probe.AliasWide", false, loader));
            final String longSpelling = " ".repeat(257) + "getAbsent";
            for (int i = 0; i < 2_000; i++)
                observed = info.getPropInfo(longSpelling);
            final long start = allocations.getThreadAllocatedBytes(Thread.currentThread().threadId());
            for (int i = 0; i < 2_000; i++)
                observed = info.getPropInfo(longSpelling);
            final long bytesPerLookup = (allocations.getThreadAllocatedBytes(Thread.currentThread().threadId()) - start) / 2_000;
            assertNull(observed);
            assertTrue(bytesPerLookup < 2_000, "temporary bytes per uncached lookup: " + bytesPerLookup);
        }
    }

    @Test
    @Tag("slow-test")
    @SuppressWarnings("deprecation")
    public void repeatedIgnoredPatternsHaveBoundedColdConstructionAllocation(@TempDir final Path directory) throws Exception {
        final com.sun.management.ThreadMXBean allocations = allocations();
        try (URLClassLoader loader = compileWideBean(directory, true)) {
            final Class<?> type = Class.forName("probe.PatternWide", false, loader);
            for (int i = 0; i < 8; i++)
                observed = ParserUtil.getBeanInfo(type, false);
            final long start = allocations.getThreadAllocatedBytes(Thread.currentThread().threadId());
            for (int i = 0; i < 8; i++)
                observed = ParserUtil.getBeanInfo(type, false);
            final long bytesPerConstruction = (allocations.getThreadAllocatedBytes(Thread.currentThread().threadId()) - start) / 8;
            assertEquals(256, ((ParserUtil.BeanInfo) observed).propInfoList.size());
            assertTrue(bytesPerConstruction < 3_500_000, "bytes per 256-property metadata construction: " + bytesPerConstruction);
        }
    }

    private static com.sun.management.ThreadMXBean allocations() {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        final com.sun.management.ThreadMXBean result = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        assumeTrue(result.isThreadAllocatedMemorySupported() && result.isThreadAllocatedMemoryEnabled());
        return result;
    }

    private static URLClassLoader compileWideBean(final Path directory, final boolean patterns) throws Exception {
        assumeTrue(ToolProvider.getSystemJavaCompiler() != null);
        final String className = patterns ? "PatternWide" : "AliasWide";
        final StringBuilder source = new StringBuilder("package probe; ");
        if (patterns)
            source.append(
                    "@com.landawn.abacus.annotation.JsonXmlConfig(ignoredFields={\"missing[A-Z0-9]+.*\",\"hidden[0-9]{2,8}.*\",\"secret(?:Value|Name).*\",\"private\\\\w+\"}) ");
        source.append("public class ").append(className).append(" {");
        for (int i = 0; i < 256; i++)
            source.append("public int property").append(i).append(';');
        source.append('}');
        final Path file = directory.resolve(className + ".java");
        Files.writeString(file, source);
        assertEquals(0, ToolProvider.getSystemJavaCompiler()
                .run(null, null, null, "--release", "21", "-classpath", System.getProperty("java.class.path"), "-d", directory.toString(), file.toString()));
        return new URLClassLoader(new java.net.URL[] { directory.toUri().toURL() }, ParserUtilMetadataPerformance20261030Test.class.getClassLoader());
    }

    public static class AliasBean {
        public String value;
        public String k;
        public String i;
        public String \uD801\uDC28;
        public boolean hasChildren;
        public boolean children;
    }

    public static class Node {
        public String value;
        public AliasBean node;
    }

    @JsonXmlConfig(ignoredFields = { "hidden.*", "secret", "computed.*" })
    public static class PatternBean {
        public int visible = 1;
        public int hiddenValue = 2;
        public int secret = 3;

        public String getComputedText() {
            return "hidden computed";
        }
    }

    @JsonXmlConfig(ignoredFields = { "value", "[" })
    public static class ExactBeforeInvalid {
        public int value;
    }

    @JsonXmlConfig(ignoredFields = "[")
    public static class FieldIgnoreBeforeInvalid {
        @JsonXmlField(ignore = true)
        public int value;
    }

    @JsonXmlConfig(ignoredFields = { "[", "value" })
    public static class InvalidBeforeExact {
        public int value;
    }

    @JsonXmlConfig(ignoredFields = "[")
    public static class UnmatchedInvalid {
        public int other;
    }
}
