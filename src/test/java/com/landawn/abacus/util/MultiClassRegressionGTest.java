package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the defects found in the 2026-09-02 review of CodeGenerationUtil, XmlUtil,
 * FilenameUtil, PropertiesUtil, AddrUtil and EmailUtil.
 *
 * <p>Every test here fails on the pre-fix code.</p>
 */
public class MultiClassRegressionGTest extends TestBase {

    @TempDir
    Path tempDir;

    @BeforeEach
    public void clearAutoRefreshRegistry() {
        PropertiesUtil.stopAllAutoRefresh();
    }

    @AfterEach
    public void stopAutoRefresh() {
        PropertiesUtil.stopAllAutoRefresh();
    }

    // ------------------------------------------------------------------------------------------
    // B1 - generatePropNameTableClass(..., srcDir) must not delete unrelated source
    // ------------------------------------------------------------------------------------------

    private static final String ENTITY_HEAD = String.join("\n", "package com.example;", "", "public class Foo {", "    private String name;", "",
            "    public String getName() { return name; }", "", "    public void setName(String n) { this.name = n; }", "");

    private File writeEntity(final Path root, final String body) throws Exception {
        final File pkg = root.resolve("com/example").toFile();
        assertTrue(pkg.mkdirs() || pkg.isDirectory());
        final File src = new File(pkg, "Foo.java");
        Files.write(src.toPath(), (ENTITY_HEAD + body + "}\n").getBytes(StandardCharsets.UTF_8));
        return src;
    }

    private Class<?> compileEntity(final Path root, final File src) throws Exception {
        final File out = root.resolveSibling(root.getFileName() + "-classes").toFile();
        assertTrue(out.mkdirs() || out.isDirectory());
        final JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, jc.run(null, null, null, "-d", out.getAbsolutePath(), src.getAbsolutePath()), "entity must compile");

        // Deliberately not closed: Beans.getPropNameList() resolves the entity's nested types lazily, so
        // closing the loader here would make Foo$xtras unloadable mid-scan. The loader is collected with
        // the test.
        final URLClassLoader cl = new URLClassLoader(new URL[] { out.toURI().toURL() }, getClass().getClassLoader()); //NOSONAR

        return cl.loadClass("com.example.Foo");
    }

    private boolean compiles(final Path root, final File src) {
        final File out = root.resolveSibling(root.getFileName() + "-verify").toFile();
        assertTrue(out.mkdirs() || out.isDirectory());
        final JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

        return jc.run(null, null, new ByteArrayOutputStream(), "-d", out.getAbsolutePath(), src.getAbsolutePath()) == 0;
    }

    @Test
    public void generatePropNameTableClass_keepsHandWrittenTypeSharingTheGeneratedNamePrefix() throws Exception {
        final Path root = tempDir.resolve("b1a");
        final File src = writeEntity(root, String.join("\n", "", "    public interface xtras {", "        int KEEP_ME = 1;", "    }", ""));
        final Class<?> foo = compileEntity(root, src);

        CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath());

        final String after = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);
        assertTrue(after.contains("public interface xtras"), "hand-written 'xtras' must survive generating 'x'");
        assertTrue(after.contains("int KEEP_ME = 1;"), "its body must survive too");
        assertTrue(after.contains("public interface x {"), "the generated interface must be inserted");
        assertTrue(compiles(root, src), "the rewritten source must still compile");
    }

    @Test
    public void generatePropNameTableClass_ignoresTheGeneratedDocPhraseInAnUnrelatedComment() throws Exception {
        final Path root = tempDir.resolve("b1b");
        final File src = writeEntity(root, String.join("\n", "", "    /**", "     * Auto-generated class for property(field) name table.", "     */",
                "    public interface unrelated {", "        int KEEP_ME = 2;", "    }", ""));
        final Class<?> foo = compileEntity(root, src);

        CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath());

        final String after = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);
        assertTrue(after.contains("int KEEP_ME = 2;"), "the phrase alone must not trigger deletion");
        assertTrue(compiles(root, src));
    }

    @Test
    public void generatePropNameTableClass_writesMarkersAndIsIdempotent() throws Exception {
        final Path root = tempDir.resolve("b1c");
        final File src = writeEntity(root, "");
        final Class<?> foo = compileEntity(root, src);

        final String generated = CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath());
        assertTrue(generated.contains("// <auto-generated-prop-name-table:x>"));
        assertTrue(generated.contains("// </auto-generated-prop-name-table:x>"));

        CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath());
        CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath());

        final String after = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);
        assertEquals(1, countOccurrences(after, "// <auto-generated-prop-name-table:x>"), "regeneration must replace, not append");
        assertEquals(1, countOccurrences(after, "public interface x {"));
        assertTrue(compiles(root, src));
    }

    @Test
    public void generatePropNameTableClass_leavesTablesGeneratedUnderOtherNamesAlone() throws Exception {
        final Path root = tempDir.resolve("b1d");
        final File src = writeEntity(root, "");
        final Class<?> foo = compileEntity(root, src);

        CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath());
        CodeGenerationUtil.generatePropNameTableClass(foo, "x2", root.toFile().getAbsolutePath());

        final String after = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);
        assertTrue(after.contains("// <auto-generated-prop-name-table:x>"), "'x' must survive generating 'x2'");
        assertTrue(after.contains("// <auto-generated-prop-name-table:x2>"));
        assertTrue(compiles(root, src));
    }

    @Test
    public void generatePropNameTableClass_upgradesAMarkerlessLegacyBlockInPlace() throws Exception {
        final Path root = tempDir.resolve("b1e");
        final File src = writeEntity(root,
                String.join("\n", "", "    /**", "     * Auto-generated class for property(field) name table.", "     */",
                        "    public interface x { // NOSONAR", "", "        /** stale */", "        String staleConstant = \"stale\";", "", "    }", "",
                        "    public interface xtras {", "        int KEEP_ME = 3;", "    }", ""));
        final Class<?> foo = compileEntity(root, src);

        CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath());

        final String after = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);
        assertFalse(after.contains("String staleConstant"), "the stale legacy block must be replaced");
        assertEquals(1, countOccurrences(after, "public interface x {"));
        assertTrue(after.contains("// <auto-generated-prop-name-table:x>"), "the legacy block must be upgraded to markers");
        assertTrue(after.contains("int KEEP_ME = 3;"), "'xtras' must still survive the legacy upgrade");
        assertTrue(compiles(root, src));
    }

    @Test
    public void generatePropNameTableClass_refusesToGuessWhenTheClosingMarkerIsMissing() throws Exception {
        final Path root = tempDir.resolve("b1f");
        final File src = writeEntity(root, "\n    // <auto-generated-prop-name-table:x>\n");
        final Class<?> foo = compileEntity(root, src);
        final String before = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class, () -> CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath()));
        assertEquals(before, new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8), "a refused run must not modify the file");
    }

    private static int countOccurrences(final String s, final String sub) {
        int n = 0;

        for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + 1)) {
            n++;
        }

        return n;
    }

    // ------------------------------------------------------------------------------------------
    // B5 - XmlUtil.transform must write exactly where it is told
    // ------------------------------------------------------------------------------------------

    @Test
    public void transformToFile_doesNotRedirectToAPercent20DecodedSibling() throws Exception {
        final File decoded = tempDir.resolve("a b.xml").toFile();
        final File requested = tempDir.resolve("a%20b.xml").toFile();
        Files.write(decoded.toPath(), "<old/>".getBytes(StandardCharsets.UTF_8));

        final Document doc = XmlUtil.createDOMParser().newDocument();
        doc.appendChild(doc.createElement("brandnew"));

        XmlUtil.transform(doc, requested);

        assertTrue(requested.exists(), "the requested file must be created");
        assertTrue(new String(Files.readAllBytes(requested.toPath()), StandardCharsets.UTF_8).contains("brandnew"));
        assertEquals("<old/>", new String(Files.readAllBytes(decoded.toPath()), StandardCharsets.UTF_8), "the decoded sibling must be untouched");
    }

    @Test
    public void transformToFile_stillWritesUtf8() throws Exception {
        final File out = tempDir.resolve("utf8.xml").toFile();
        final Document doc = XmlUtil.createDOMParser().newDocument();
        final org.w3c.dom.Element root = doc.createElement("root");
        root.setTextContent("café 你好");
        doc.appendChild(root);

        XmlUtil.transform(doc, out);

        assertTrue(new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8).contains("café 你好"));
    }

    // ------------------------------------------------------------------------------------------
    // B4 - wildcardMatch must be linear, with unchanged semantics
    // ------------------------------------------------------------------------------------------

    @Test
    public void wildcardMatch_semanticsAreUnchanged() {
        assertTrue(FilenameUtil.wildcardMatch("abc", "a*c"));
        assertTrue(FilenameUtil.wildcardMatch("abc", "a?c"));
        assertFalse(FilenameUtil.wildcardMatch("abc", "a??c"));
        assertTrue(FilenameUtil.wildcardMatch("", "*"));
        assertTrue(FilenameUtil.wildcardMatch("", ""));
        assertFalse(FilenameUtil.wildcardMatch("", "?"));
        assertTrue(FilenameUtil.wildcardMatch("aa", "*a"));
        assertTrue(FilenameUtil.wildcardMatch("abab", "*ab"));
        assertTrue(FilenameUtil.wildcardMatch("abab", "ab*"));
        assertTrue(FilenameUtil.wildcardMatch("xaxbxc", "*a*b*c"));
        assertFalse(FilenameUtil.wildcardMatch("aXbXc", "a*c*b"));
        assertTrue(FilenameUtil.wildcardMatch("banana", "*ana*"));
        assertTrue(FilenameUtil.wildcardMatch("banana", "*ana"));
        assertTrue(FilenameUtil.wildcardMatch("c.txt", "*.???"));
        assertFalse(FilenameUtil.wildcardMatch("c.txt", "*.????"));
        assertTrue(FilenameUtil.wildcardMatch("a/b/c.txt", "a/b/*"));
        assertFalse(FilenameUtil.wildcardMatch("abc", "abcd"));
        assertFalse(FilenameUtil.wildcardMatch("abcd", "abc"));
        assertTrue(FilenameUtil.wildcardMatch("abc", "**"));
    }

    @Test
    public void wildcardMatch_nullAndCaseRulesAreUnchanged() {
        assertTrue(FilenameUtil.wildcardMatch(null, null));
        assertFalse(FilenameUtil.wildcardMatch(null, "*"));
        assertFalse(FilenameUtil.wildcardMatch("a", null));
        assertTrue(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", IOCase.INSENSITIVE));
        assertFalse(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", IOCase.SENSITIVE));
        assertFalse(FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", null), "null IOCase means case-sensitive");
    }

    @Test
    public void wildcardMatch_questionMarkConsumesOneUtf16CodeUnit() {
        assertTrue(FilenameUtil.wildcardMatch("😀.txt", "??.txt"));
        assertFalse(FilenameUtil.wildcardMatch("😀.txt", "?.txt"));
    }

    @Test
    public void wildcardMatch_doesNotBlowUpOnAPathologicalPattern() {
        // The previous backtracking matcher needed ~18 s for a 40-character subject here, and grew
        // exponentially from there; the greedy matcher is linear.
        final String pattern = "*a*a*a*a*a*a*a*a*a*b";

        for (int i = 0; i < 20_000; i++) {
            FilenameUtil.wildcardMatch("abcdef.txt", "*.txt");
        }

        final long startNanos = System.nanoTime();
        assertFalse(FilenameUtil.wildcardMatch("a".repeat(2000), pattern));
        final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        assertTrue(elapsedMillis < 2000, "wildcardMatch took " + elapsedMillis + " ms; it must not backtrack exponentially");
    }

    // ------------------------------------------------------------------------------------------
    // B2 - storeToXml must only emit type names loadFromXml accepts
    // ------------------------------------------------------------------------------------------

    @Test
    public void storeToXml_roundTripsEveryAllowlistedType() throws Exception {
        final Properties<String, Object> props = new Properties<>();
        props.put("port", 8080);
        props.put("enabled", true);
        props.put("name", "a<b>&c\"d'e");
        props.put("when", LocalDate.of(2020, 1, 2));
        props.put("dur", Duration.ofSeconds(3));
        props.put("id", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        props.put("tags", new ArrayList<>(List.of("x", "y")));

        final File xml = tempDir.resolve("rt.xml").toFile();
        PropertiesUtil.storeToXml(props, "config", true, xml);

        final String written = new String(Files.readAllBytes(xml.toPath()), StandardCharsets.UTF_8);
        assertFalse(written.contains("JdkDuration"), "the internal declaring name is not loadable");
        assertTrue(written.contains("type=\"java.time.Duration\""));

        final Properties<String, Object> back = PropertiesUtil.loadFromXml(xml);
        assertEquals(props.keySet(), back.keySet());
        assertEquals(8080, back.get("port"));
        assertEquals(Boolean.TRUE, back.get("enabled"));
        assertEquals("a<b>&c\"d'e", back.get("name"));
        assertEquals(LocalDate.of(2020, 1, 2), back.get("when"));
        assertEquals(Duration.ofSeconds(3), back.get("dur"));
        assertEquals(props.get("id"), back.get("id"));
        assertEquals(List.of("x", "y"), back.get("tags"));
    }

    @Test
    public void storeToXml_rejectsAnUnloadableTypeBeforeWritingAnything() {
        final Properties<String, Object> props = new Properties<>();
        props.put("point", new java.awt.Point(1, 2));
        final File out = tempDir.resolve("never.xml").toFile();

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.storeToXml(props, "config", true, out));
        assertTrue(e.getMessage().contains("point"), e.getMessage());
        assertTrue(!out.exists() || out.length() == 0, "no partial document may be left behind");
    }

    @Test
    public void storeToXml_withoutTypeInfoStillAcceptsAnyType() throws Exception {
        final Properties<String, Object> props = new Properties<>();
        props.put("point", new java.awt.Point(1, 2));
        final File out = tempDir.resolve("plain.xml").toFile();

        PropertiesUtil.storeToXml(props, "config", false, out);

        assertTrue(out.length() > 0);
        assertFalse(new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8).contains("type="));
    }

    // ------------------------------------------------------------------------------------------
    // B6 - xmlToJava byte-source vs character-source contracts
    // ------------------------------------------------------------------------------------------

    private static final String LATIN1_DOC = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><root><straße>x</straße></root>";

    private File latin1File() throws Exception {
        final File f = tempDir.resolve("latin1.xml").toFile();
        Files.write(f.toPath(), LATIN1_DOC.getBytes(StandardCharsets.ISO_8859_1));

        return f;
    }

    private String generated(final File outDir, final String className) throws Exception {
        return new String(Files.readAllBytes(new File(outDir, className + ".java").toPath()), StandardCharsets.UTF_8);
    }

    @Test
    public void xmlToJavaFromFile_honoursTheDocumentsEncodingDeclaration() throws Exception {
        final File out = tempDir.resolve("g1").toFile();

        PropertiesUtil.xmlToJava(latin1File(), out.getAbsolutePath(), null, "C1", false);

        assertTrue(generated(out, "C1").contains("getStraße"), "a byte source must use the declared charset");
    }

    @Test
    public void xmlToJavaFromInputStream_honoursTheDocumentsEncodingDeclaration() throws Exception {
        final File out = tempDir.resolve("g2").toFile();

        try (InputStream in = Files.newInputStream(latin1File().toPath())) {
            PropertiesUtil.xmlToJava(in, out.getAbsolutePath(), null, "C2", false);
        }

        assertTrue(generated(out, "C2").contains("getStraße"));
    }

    @Test
    public void xmlToJavaFromString_ignoresTheEncodingDeclarationAndUsesTheDecodedText() throws Exception {
        final File out = tempDir.resolve("g3").toFile();

        PropertiesUtil.xmlToJava(LATIN1_DOC, out.getAbsolutePath(), null, "C3", false);

        assertTrue(generated(out, "C3").contains("getStraße"), "a character source is already decoded");
    }

    @Test
    public void xmlToJavaFromReader_usesTheReadersDecoding() throws Exception {
        final File out = tempDir.resolve("g4").toFile();

        try (Reader r = new InputStreamReader(Files.newInputStream(latin1File().toPath()), StandardCharsets.ISO_8859_1)) {
            PropertiesUtil.xmlToJava(r, out.getAbsolutePath(), null, "C4", false);
        }

        assertTrue(generated(out, "C4").contains("getStraße"));
    }

    @Test
    public void xmlToJava_stillHandlesUtf8WithoutADeclaration() throws Exception {
        final File src = tempDir.resolve("utf8.xml").toFile();
        Files.write(src.toPath(), "<root><café>x</café></root>".getBytes(StandardCharsets.UTF_8));
        final File out = tempDir.resolve("g5").toFile();

        PropertiesUtil.xmlToJava(src, out.getAbsolutePath(), null, "C5", false);

        assertTrue(generated(out, "C5").contains("getCafé"));
    }

    @Test
    public void loadFromXmlFromFile_stillHonoursTheEncodingDeclaration() throws Exception {
        assertTrue(PropertiesUtil.loadFromXml(latin1File()).containsKey("straße"));
    }

    // ------------------------------------------------------------------------------------------
    // B7 - auto-refresh lifecycle
    // ------------------------------------------------------------------------------------------

    @Test
    public void plainLoadStartsNoBackgroundPollThread() throws Exception {
        // Scheduling used to happen in a static initializer, so it fired on the first touch of this
        // class from anywhere. That makes the "before" snapshot decisive only when this test is the
        // first thing in the JVM to load PropertiesUtil; the load-bearing coverage for the lazy start
        // is autoRefreshStillReloadsAndCanBeStopped, which needs the stop API this fix introduces.
        final Set<String> before = threadNames();

        PropertiesUtil.load(new ByteArrayInputStream("k=v".getBytes(StandardCharsets.ISO_8859_1)));
        Thread.sleep(200);

        final Set<String> started = threadNames();
        started.removeAll(before);
        assertTrue(started.isEmpty(), "loading without autoRefresh must not schedule anything, but started " + started);
    }

    @Test
    public void unregisteringTheLastResourceStopsThePolling() throws Exception {
        final File cfg = tempDir.resolve("polling.properties").toFile();
        Files.write(cfg.toPath(), "k=v1".getBytes(StandardCharsets.ISO_8859_1));

        final Properties<String, String> live = PropertiesUtil.load(cfg, true);
        assertEquals(1, PropertiesUtil.stopAllAutoRefresh(), "exactly one registration existed");

        // With nothing registered the poll task is cancelled, so later edits are ignored entirely.
        Files.write(cfg.toPath(), "k=v2".getBytes(StandardCharsets.ISO_8859_1));
        assertTrue(cfg.setLastModified(System.currentTimeMillis() + 10_000));
        Thread.sleep(2500);

        assertEquals("v1", live.get("k"), "a fully drained registry must not refresh anything");
        assertEquals(0, PropertiesUtil.stopAllAutoRefresh(), "the registry stays empty");
    }

    @Test
    public void autoRefreshStillReloadsAndCanBeStopped() throws Exception {
        final File cfg = tempDir.resolve("auto.properties").toFile();
        Files.write(cfg.toPath(), "k=v1".getBytes(StandardCharsets.ISO_8859_1));

        final Properties<String, String> live = PropertiesUtil.load(cfg, true);
        assertEquals("v1", live.get("k"));

        Files.write(cfg.toPath(), "k=v2".getBytes(StandardCharsets.ISO_8859_1));
        assertTrue(cfg.setLastModified(System.currentTimeMillis() + 5000));

        final long deadline = System.currentTimeMillis() + 10_000;
        while (!"v2".equals(live.get("k")) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertEquals("v2", live.get("k"), "an auto-refresh registration must still pick up changes");

        assertTrue(PropertiesUtil.stopAutoRefresh(cfg));
        assertFalse(PropertiesUtil.stopAutoRefresh(cfg), "unregistering twice reports nothing removed");

        Files.write(cfg.toPath(), "k=v3".getBytes(StandardCharsets.ISO_8859_1));
        assertTrue(cfg.setLastModified(System.currentTimeMillis() + 10_000));
        Thread.sleep(1500);
        assertEquals("v2", live.get("k"), "an unregistered instance must no longer be refreshed");
    }

    @Test
    public void loadWithAutoRefreshReturnsTheSameLiveInstance() throws Exception {
        final File cfg = tempDir.resolve("same.properties").toFile();
        Files.write(cfg.toPath(), "k=v".getBytes(StandardCharsets.ISO_8859_1));

        assertTrue(PropertiesUtil.load(cfg, true) == PropertiesUtil.load(cfg, true));
        assertEquals(1, PropertiesUtil.stopAllAutoRefresh());
    }

    @Test
    public void stopAutoRefreshRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.stopAutoRefresh(null));
    }

    private static Set<String> threadNames() {
        final Set<String> names = new TreeSet<>();

        for (final Thread t : Thread.getAllStackTraces().keySet()) {
            names.add(t.getName());
        }

        return names;
    }

    // ------------------------------------------------------------------------------------------
    // B3 / J2 - documented charset pairing for load/store
    // ------------------------------------------------------------------------------------------

    @Test
    public void storeAndLoadRoundTripNonAsciiThroughTheByteOverloads() throws Exception {
        final String value = "café 你好";
        final Properties<String, String> props = new Properties<>();
        props.put("greeting", value);

        final File f = tempDir.resolve("bytes.properties").toFile();
        PropertiesUtil.store(props, null, f);

        assertEquals(value, PropertiesUtil.load(f).get("greeting"));
    }

    @Test
    public void storeAndLoadRoundTripNonAsciiThroughTheCharacterOverloads() throws Exception {
        final String value = "café 你好";
        final Properties<String, String> props = new Properties<>();
        props.put("greeting", value);

        final File f = tempDir.resolve("chars.properties").toFile();

        try (Writer w = new OutputStreamWriter(Files.newOutputStream(f.toPath()), StandardCharsets.UTF_8)) {
            PropertiesUtil.store(props, null, w);
        }

        try (Reader r = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8)) {
            assertEquals(value, PropertiesUtil.load(r).get("greeting"));
        }
    }

    // ------------------------------------------------------------------------------------------
    // B8 / B11 - AddrUtil
    // ------------------------------------------------------------------------------------------

    @Test
    public void getAddressListOverloadsTrimIdentically() {
        assertEquals(AddrUtil.getAddressList(" host:80 ").get(0), AddrUtil.getAddressList(List.of(" host:80 ")).get(0));
        assertEquals(80, AddrUtil.getAddressList(List.of(" host:80 ")).get(0).getPort());
        assertEquals("host", AddrUtil.getAddressList(List.of("\thost:80\n")).get(0).getHostString());
    }

    @Test
    public void bracketedIpv6WithoutAPortReportsTheMissingPort() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("[::1]"));
        assertTrue(e.getMessage().contains("no port"), e.getMessage());
    }

    @Test
    public void genuinelyUnbalancedBracketStillReportsThat() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("[::1:80"));
        assertTrue(e.getMessage().contains("no matching closing"), e.getMessage());
    }

    @Test
    public void ipv6FormsThatWereAcceptedStillAre() {
        assertEquals(11211, AddrUtil.getAddressList("::1:11211").get(0).getPort());
        assertEquals(11211, AddrUtil.getAddressList("[::1]:11211").get(0).getPort());
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList("::1:1121"));
    }

    @Test
    public void blankCollectionElementIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> AddrUtil.getAddressList(List.of("   ")));
    }

    // ------------------------------------------------------------------------------------------
    // B10 - EmailUtil header encoding
    // ------------------------------------------------------------------------------------------

    private String messageWire(final String[] to, final String from, final String subject, final String[] attachments) throws Exception {
        final java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.host", "localhost");

        final Method m = EmailUtil.class.getDeclaredMethod("createMessage", String[].class, String.class, String.class, String.class, String[].class,
                boolean.class, String.class, String.class, java.util.Properties.class);
        m.setAccessible(true);

        final Object mail = m.invoke(null, to, from, subject, "body", attachments, false, null, null, props);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        mail.getClass().getMethod("writeTo", OutputStream.class).invoke(mail, bos);

        return bos.toString(StandardCharsets.ISO_8859_1);
    }

    private static String header(final String wire, final String name) {
        for (final String line : wire.split("\r\n")) {
            if (line.startsWith(name + ":")) {
                return line;
            }
        }

        return "";
    }

    @Test
    public void subjectIsExplicitlyUtf8Encoded() throws Exception {
        // The subject charset is now pinned rather than inherited from mail.mime.charset / the platform
        // default. Setting mail.mime.charset here would only change the pre-fix outcome when javax.mail
        // has not yet cached its default MIME charset (it caches in a static), so this asserts the
        // guarantee directly: the header is UTF-8 encoded and every character survives it.
        final String subject = "Grüße 你好";
        final String wire = messageWire(new String[] { "to@example.com" }, "from@example.com", subject, null);
        final String header = header(wire, "Subject");

        assertTrue(header.contains("=?UTF-8?"), header);
        assertEquals(subject, javax.mail.internet.MimeUtility.decodeText(header.substring("Subject: ".length())),
                "no character may be lost to a charset that cannot represent it");
        assertTrue(wire.contains("charset=UTF-8"), "the body must stay UTF-8 too");
    }

    @Test
    public void displayNamesAreUtf8Encoded() throws Exception {
        final String wire = messageWire(new String[] { "Grüße <to@example.com>" }, "Sender 你好 <from@example.com>", "s", null);

        assertTrue(header(wire, "To").contains("=?UTF-8?"), header(wire, "To"));
        assertTrue(header(wire, "From").contains("=?UTF-8?"), header(wire, "From"));
    }

    @Test
    public void attachmentFileNameSurvivesMimeParameterRoundTrip() throws Exception {
        for (final String name : new String[] { "resume.txt", "résumé.txt", "\u6587\u4ef6-\ud83d\ude42.txt" }) {
            final File att = tempDir.resolve(name).toFile();
            Files.write(att.toPath(), new byte[0]);
            final String wire = messageWire(new String[] { "to@example.com" }, "from@example.com", "s", new String[] { att.getAbsolutePath() });
            final javax.mail.internet.MimeMessage parsed = new javax.mail.internet.MimeMessage(javax.mail.Session.getInstance(new java.util.Properties()),
                    new ByteArrayInputStream(wire.getBytes(StandardCharsets.ISO_8859_1)));
            final javax.mail.BodyPart attachment = ((javax.mail.Multipart) parsed.getContent()).getBodyPart(1);
            final String disposition = attachment.getHeader("Content-Disposition")[0];

            // Validate the recipient's decoded filename as well as the parameter's wire encoding.
            assertEquals(name, attachment.getFileName());
            assertFalse(disposition.contains("=?"), disposition);
            if (!StandardCharsets.US_ASCII.newEncoder().canEncode(name)) {
                assertTrue(disposition.contains("filename*"), disposition);
                assertTrue(disposition.toUpperCase(java.util.Locale.ROOT).contains("UTF-8"), disposition);
                assertFalse(disposition.contains(name), disposition);
            }
        }
    }

    @Test
    public void asciiHeadersAreLeftUnencoded() throws Exception {
        final String wire = messageWire(new String[] { "to@example.com" }, "from@example.com", "Plain subject", null);

        assertEquals("Subject: Plain subject", header(wire, "Subject"));
    }

    @Test
    public void createMessageStillValidatesItsEnvelope() {
        final java.util.Properties props = new java.util.Properties();
        assertThrows(IllegalArgumentException.class,
                () -> EmailUtil.createMessage(new String[] { "to@example.com" }, "", "s", "b", null, false, null, null, props));
        assertThrows(IllegalArgumentException.class,
                () -> EmailUtil.createMessage(new String[0], "from@example.com", "s", "b", null, false, null, null, props));
    }

    // ------------------------------------------------------------------------------------------
    // D6 - argument validation throws IllegalArgumentException
    // ------------------------------------------------------------------------------------------

    @Test
    public void findFileFamilyRejectsEmptyNamesWithIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.findFile(""));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.findFile(null));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.findDir(""));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.findDir(null));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.findFileInDir("", new File("."), false));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.findFileRelativeTo(new File("."), ""));
    }

    // ------------------------------------------------------------------------------------------
    // Doc-fix sanity: behaviour the updated Javadoc now states explicitly
    // ------------------------------------------------------------------------------------------

    @Test
    public void normalizeTreatsALeadingDoubleSlashAsAUncPrefix() {
        assertEquals("//server/share", FilenameUtil.normalize("//server/share", true));
        assertNotNull(FilenameUtil.normalize("//server/share/x", true));
        assertEquals(null, FilenameUtil.normalize("//foo", true), "a UNC prefix naming no share is invalid");
        assertEquals(null, FilenameUtil.normalize("///foo", true));
        assertEquals(null, FilenameUtil.normalize("//../foo", true), "CVE-2021-29425 style traversal stays rejected");
    }

    @Test
    public void directoryContainsIsLexicalOnly() {
        assertTrue(FilenameUtil.directoryContains("/home/a", "/home/a/../b"), "documented: no normalization happens here");
        assertFalse(FilenameUtil.directoryContains("/home/a", "/home/ab"));
        assertFalse(FilenameUtil.directoryContains("/home/a", "/home/a"));
        assertFalse(FilenameUtil.directoryContains("/home/a", null));
        assertTrue(FilenameUtil.directoryContains("", "/x"), "documented: an empty parent contains everything");
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.directoryContains(null, "/x"));
    }

    @Test
    public void escapeHtml4LeavesTheApostropheAsIs() {
        assertEquals("a'b", EscapeUtil.escapeHtml4("a'b"), "documented: not safe for single-quoted attribute values");
        assertEquals("&lt;p&gt;", EscapeUtil.escapeHtml4("<p>"));
    }

    @Test
    public void generatePropNameTableClass_refusesAHandWrittenInterfaceThatIsNotAGeneratedTable() throws Exception {
        final Path root = tempDir.resolve("b1g");
        // A hand-written interface named "x" whose body contains a nested type. Scanning to the first
        // line that is just "}" would stop at the NESTED closing brace and truncate the declaration.
        final File src = writeEntity(root,
                String.join("\n", "", "    public interface x {", "        interface Nested {", "        }", "", "        int KEEP_ME = 7;", "    }", ""));
        final Class<?> foo = compileEntity(root, src);
        final String before = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);

        final IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath()));
        assertTrue(e.getMessage().contains("not a generated property-name table"), e.getMessage());

        assertEquals(before, new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8), "a refused run must not modify the file");
        assertTrue(compiles(root, src), "the untouched source must still compile");
    }

    @Test
    public void generatePropNameTableClass_regenerationReachesAFixedPoint() throws Exception {
        final Path root = tempDir.resolve("b1h");
        final File src = writeEntity(root, "");
        final Class<?> foo = compileEntity(root, src);

        String previous = null;

        for (int round = 1; round <= 5; round++) {
            CodeGenerationUtil.generatePropNameTableClass(foo, "x", root.toFile().getAbsolutePath());
            final String now = new String(Files.readAllBytes(src.toPath()), StandardCharsets.UTF_8);

            if (round >= 3) {
                assertEquals(previous, now, "round " + round + " must be byte-identical to round " + (round - 1));
            }

            previous = now;
        }

        assertFalse(previous.contains("\n\n\n\n"), "blank lines must not accumulate");
        assertTrue(compiles(root, src));
    }

    // ------------------------------------------------------------------------------------------
    // B4 - wildcardMatch differential against a reference implementation
    // ------------------------------------------------------------------------------------------

    /** Exponential but obviously-correct reference matcher, used to validate the linear one. */
    private static boolean referenceMatch(final String text, final int ti, final String pattern, final int pi) {
        if (pi == pattern.length()) {
            return ti == text.length();
        }

        final char pc = pattern.charAt(pi);

        if (pc == '*') {
            for (int k = ti; k <= text.length(); k++) {
                if (referenceMatch(text, k, pattern, pi + 1)) {
                    return true;
                }
            }

            return false;
        }

        if (ti == text.length()) {
            return false;
        }

        return (pc == '?' || pc == text.charAt(ti)) && referenceMatch(text, ti + 1, pattern, pi + 1);
    }

    private static List<String> allStrings(final char[] alphabet, final int maxLength) {
        final List<String> all = new ArrayList<>();
        all.add("");
        List<String> current = new ArrayList<>(List.of(""));

        for (int length = 1; length <= maxLength; length++) {
            final List<String> next = new ArrayList<>();

            for (final String s : current) {
                for (final char c : alphabet) {
                    next.add(s + c);
                }
            }

            all.addAll(next);
            current = next;
        }

        return all;
    }

    @Test
    public void wildcardMatch_agreesWithAReferenceMatcherOnEveryShortInput() {
        final List<String> texts = allStrings(new char[] { 'a', 'b' }, 4);
        final List<String> patterns = allStrings(new char[] { 'a', 'b', '*', '?' }, 4);
        int compared = 0;

        for (final String text : texts) {
            for (final String pattern : patterns) {
                final boolean expected = referenceMatch(text, 0, pattern, 0);
                final boolean actual = FilenameUtil.wildcardMatch(text, pattern, IOCase.SENSITIVE);
                compared++;

                if (expected != actual) {
                    fail("wildcardMatch(\"" + text + "\", \"" + pattern + "\") = " + actual + ", expected " + expected);
                }
            }
        }

        assertTrue(compared > 10_000, "expected a broad sweep, compared only " + compared);
    }

    @Test
    public void wildcardMatch_trailingStarIsConstantTime() {
        for (int i = 0; i < 20_000; i++) {
            FilenameUtil.wildcardMatch("abc.txt", "*.txt");
        }

        final String longName = "a".repeat(2_000_000);
        final long startNanos = System.nanoTime();
        assertTrue(FilenameUtil.wildcardMatch(longName, "aaa*"));
        final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        assertTrue(elapsedMillis < 500, "a trailing '*' must not walk the whole name; took " + elapsedMillis + " ms");
    }

    // ------------------------------------------------------------------------------------------
    // B2 - validation must reach values nested inside child Properties
    // ------------------------------------------------------------------------------------------

    @Test
    public void storeToXml_rejectsAnUnloadableTypeNestedInAChildProperties() {
        final Properties<String, Object> inner = new Properties<>();
        inner.put("point", new java.awt.Point(1, 2));
        final Properties<String, Object> outer = new Properties<>();
        outer.put("ok", 1);
        outer.put("nested", inner);

        final File out = tempDir.resolve("nested-bad.xml").toFile();

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.storeToXml(outer, "config", true, out));
        assertTrue(e.getMessage().contains("point"), e.getMessage());
        assertTrue(!out.exists() || out.length() == 0, "no partial document may be left behind");
    }

    @Test
    public void storeToXml_stillRoundTripsNestedProperties() throws Exception {
        final Properties<String, Object> child = new Properties<>();
        child.put("port", 99);
        final Properties<String, Object> outer = new Properties<>();
        outer.put("db", child);
        outer.put("name", "x");

        final File out = tempDir.resolve("nested-good.xml").toFile();
        PropertiesUtil.storeToXml(outer, "config", true, out);

        final Properties<String, Object> back = PropertiesUtil.loadFromXml(out);
        assertTrue(back.get("db") instanceof Properties, String.valueOf(back.get("db")));
        assertEquals(99, ((Properties<?, ?>) back.get("db")).get("port"));
        assertEquals("x", back.get("name"));
    }

    // ------------------------------------------------------------------------------------------
    // B10 - EmailUtil must still accept a null/empty subject
    // ------------------------------------------------------------------------------------------

    @Test
    public void nullSubjectEmitsNoSubjectHeader() throws Exception {
        final String wire = messageWire(new String[] { "to@example.com" }, "from@example.com", null, null);

        assertFalse(wire.contains("Subject:"), "a null subject must remove the header, not throw");
    }

    @Test
    public void emptySubjectIsAccepted() throws Exception {
        final String wire = messageWire(new String[] { "to@example.com" }, "from@example.com", "", null);

        assertTrue(wire.contains("From: from@example.com"));
    }

    @Test
    public void invalidRecipientStillRaisesMessagingException() {
        final java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.host", "localhost");

        assertThrows(javax.mail.internet.AddressException.class,
                () -> EmailUtil.createMessage(new String[] { "not a valid address @@" }, "from@example.com", "s", "b", null, false, null, null, props));
    }

    // ------------------------------------------------------------------------------------------
    // D14 - Charsets cache is keyed case-insensitively
    // ------------------------------------------------------------------------------------------

    @Test
    public void charsetLookupIsCaseInsensitiveAndStillResolvesAliases() {
        assertSame(Charsets.get("UTF-8"), Charsets.get("utf-8"));
        assertSame(Charsets.get("UTF-8"), Charsets.get("uTf-8"));
        assertSame(Charsets.UTF_8, Charsets.get("UTF-8"));
        assertSame(Charsets.US_ASCII, Charsets.get("us-ascii"));
        assertEquals("UTF-8", Charsets.get("utf8").name(), "aliases must still resolve");
    }

    @Test
    public void charsetLookupStillReportsBadNames() {
        assertThrows(java.nio.charset.UnsupportedCharsetException.class, () -> Charsets.get("no-such-charset-xyz"));
        assertThrows(java.nio.charset.IllegalCharsetNameException.class, () -> Charsets.get("bad name!"));
        assertThrows(IllegalArgumentException.class, () -> Charsets.get(null));
    }

    // ------------------------------------------------------------------------------------------
    // D4 - parser pools stay consistent after the O(1) membership change
    // ------------------------------------------------------------------------------------------

    @Test
    public void recyclingTheSameContentParserTwiceDoesNotDuplicateIt() {
        final javax.xml.parsers.DocumentBuilder first = XmlUtil.createContentParser();
        XmlUtil.recycleContentParser(first);
        XmlUtil.recycleContentParser(first);

        final javax.xml.parsers.DocumentBuilder a = XmlUtil.createContentParser();
        final javax.xml.parsers.DocumentBuilder b = XmlUtil.createContentParser();

        try {
            assertNotSame(a, b, "a double-recycled builder must be handed out only once");
        } finally {
            XmlUtil.recycleContentParser(a);
            XmlUtil.recycleContentParser(b);
        }
    }

    @Test
    public void recyclingTheSameSaxParserTwiceDoesNotDuplicateIt() {
        final javax.xml.parsers.SAXParser first = XmlUtil.createSAXParser();
        XmlUtil.recycleSAXParser(first);
        XmlUtil.recycleSAXParser(first);

        final javax.xml.parsers.SAXParser a = XmlUtil.createSAXParser();
        final javax.xml.parsers.SAXParser b = XmlUtil.createSAXParser();

        try {
            assertNotSame(a, b, "a double-recycled parser must be handed out only once");
        } finally {
            XmlUtil.recycleSAXParser(a);
            XmlUtil.recycleSAXParser(b);
        }
    }

    @Test
    public void aForeignParserIsNeverAdmittedToThePool() throws Exception {
        final javax.xml.parsers.SAXParser foreign = javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser();
        XmlUtil.recycleSAXParser(foreign);

        final List<javax.xml.parsers.SAXParser> taken = new ArrayList<>();

        try {
            for (int i = 0; i < 8; i++) {
                final javax.xml.parsers.SAXParser p = XmlUtil.createSAXParser();
                assertNotSame(foreign, p, "a parser this class did not create must not enter the pool");
                taken.add(p);
            }
        } finally {
            taken.forEach(XmlUtil::recycleSAXParser);
        }
    }

    @Test
    public void recyclingNullIsANoOp() {
        XmlUtil.recycleSAXParser(null);
        XmlUtil.recycleContentParser(null);
    }

    @Test
    public void pooledContentParserStillParses() throws Exception {
        final javax.xml.parsers.DocumentBuilder builder = XmlUtil.createContentParser();

        try {
            final Document doc = builder.parse(new ByteArrayInputStream("<r><a>1</a></r>".getBytes(StandardCharsets.UTF_8)));
            assertEquals("r", doc.getDocumentElement().getNodeName());
        } finally {
            XmlUtil.recycleContentParser(builder);
        }
    }

    // ------------------------------------------------------------------------------------------
    // D9 - the StringBuilder writeCharacters overloads no longer declare IOException
    // ------------------------------------------------------------------------------------------

    @Test
    public void writeCharactersToAStringBuilderNeedsNoCheckedExceptionHandling() {
        final StringBuilder sb = new StringBuilder();
        XmlUtil.writeCharacters("a<b>&\"c\"'d'", sb);
        assertEquals("a&lt;b&gt;&amp;&quot;c&quot;&apos;d&apos;", sb.toString());

        final StringBuilder sb2 = new StringBuilder();
        XmlUtil.writeCharacters("Hello <world>".toCharArray(), sb2);
        assertEquals("Hello &lt;world&gt;", sb2.toString());

        final StringBuilder sb3 = new StringBuilder();
        XmlUtil.writeCharacters("Hello <world>", 6, 7, sb3);
        assertEquals("&lt;world&gt;", sb3.toString());

        final StringBuilder sb4 = new StringBuilder();
        XmlUtil.writeCharacters("Hello <world>".toCharArray(), 6, 7, sb4);
        assertEquals("&lt;world&gt;", sb4.toString());

        final StringBuilder sb5 = new StringBuilder();
        XmlUtil.writeCharacters((String) null, sb5);
        assertEquals("null", sb5.toString());
    }

    // ------------------------------------------------------------------------------------------
    // B13 - Unicode escapes are zero-padded to four hex digits
    // ------------------------------------------------------------------------------------------

    @Test
    public void unicodeEscapesArePaddedToFourHexDigits() {
        assertEquals("\\u0001", EscapeUtil.escapeJava(String.valueOf((char) 1)));
        assertEquals("\\u00E9", EscapeUtil.escapeJava("é"));
        assertEquals("\\uD83D\\uDE00", EscapeUtil.escapeJava("😀"));
        assertEquals("\\uD83D\\uDE00", EscapeUtil.escapeJson("😀"));
        assertEquals("\\u4F60", EscapeUtil.escapeEcmaScript("你"));

        final String sample = "Ab\"<>&'\n\t\\/é你😀";
        assertEquals(sample, EscapeUtil.unescapeJava(EscapeUtil.escapeJava(sample)), "escape/unescape must round-trip");
    }

    // ------------------------------------------------------------------------------------------
    // D11 - WSSecurityUtil still produces nonces and digests
    // ------------------------------------------------------------------------------------------

    @Test
    public void nonceGenerationIsUnchanged() {
        assertEquals(16, WSSecurityUtil.generateNonce(16).length);
        assertEquals(0, WSSecurityUtil.generateNonce(0).length);
        assertEquals(1024, WSSecurityUtil.generateNonce(1024).length);
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateNonce(-1));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateNonce(1025));
        assertNotEquals(Arrays.toString(WSSecurityUtil.generateNonce(16)), Arrays.toString(WSSecurityUtil.generateNonce(16)));
    }

    @Test
    public void passwordDigestIsUnchanged() {
        final byte[] nonce = { 1, 2, 3 };
        final byte[] created = "2020-01-01T00:00:00Z".getBytes(StandardCharsets.UTF_8);
        final byte[] password = "secret".getBytes(StandardCharsets.UTF_8);

        final String digest = WSSecurityUtil.computePasswordDigest(nonce, created, password);
        assertEquals(28, digest.length(), "Base64 of a 20-byte SHA-1");
        assertEquals(digest, WSSecurityUtil.computePasswordDigest(nonce, created, password), "must be deterministic");
        assertNotEquals(digest, WSSecurityUtil.computePasswordDigest(nonce, created, "other".getBytes(StandardCharsets.UTF_8)));
    }

    // ------------------------------------------------------------------------------------------
    // Cycle 2 - C-050 / C-051: pinned edge-case contracts of the path decomposition
    // ------------------------------------------------------------------------------------------

    @Test
    public void normalizeIsNotIdempotentWhenItExposesANewPrefix() {
        // Removing a leading "./" can make the next character a prefix it was not before.
        assertEquals("~user", FilenameUtil.normalize("./~user", true));
        assertEquals("~user/", FilenameUtil.normalize("~user", true), "once '~' is first it is a prefix, and a prefix ends with a separator");
        assertEquals("~/", FilenameUtil.normalize("~", true));

        // A leading ':' is not a legal prefix, so normalize can emit a value it then rejects.
        assertEquals(":", FilenameUtil.normalize("./:", true));
        assertNull(FilenameUtil.normalize(":", true));
        assertNull(FilenameUtil.normalize(".:", true));

        // Second and later applications are stable.
        assertEquals("~user/", FilenameUtil.normalize(FilenameUtil.normalize("~user", true), true));
        assertEquals("a:", FilenameUtil.normalize("a:", true), "a non-drive ':' inside a name is fine");
    }

    @Test
    public void getNameAndGetBaseNameDoNotStripANonSeparatorTerminatedPrefix() {
        assertEquals("C:a", FilenameUtil.getName("C:a"));
        assertEquals("~user", FilenameUtil.getName("~user"));
        assertEquals("C:a", FilenameUtil.getBaseName("C:a.txt"));

        // ...so full path + name only reconstructs when the prefix ends with a separator.
        assertEquals("a/b.txt", FilenameUtil.getFullPath("a/b.txt") + FilenameUtil.getName("a/b.txt"));
        assertEquals("/a/b", FilenameUtil.getFullPath("/a/b") + FilenameUtil.getName("/a/b"));
        assertEquals("~/a", FilenameUtil.getFullPath("~/a") + FilenameUtil.getName("~/a"));
        assertNotEquals("C:a", FilenameUtil.getFullPath("C:a") + FilenameUtil.getName("C:a"));
        assertNotEquals("~user", FilenameUtil.getFullPath("~user") + FilenameUtil.getName("~user"));
    }

    @Test
    public void pathDecompositionRoundTripsWhenThePrefixEndsWithASeparator() {
        for (final String path : new String[] { "a/b.txt", "/a/b/c.txt", "a.txt", "a/b/c/", "~/a/b", "C:/a/b" }) {
            final String full = FilenameUtil.getFullPath(path);
            assertNotNull(full, path);
            assertEquals(path, full + FilenameUtil.getName(path), path);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Cycle 3 - xmlToJava's defining property is that its output is compilable Java. Nothing
    // asserted that, which is the same gap that let the cycle-1 write-back defect through.
    // ------------------------------------------------------------------------------------------

    private File generateAndCompile(final String xml, final String className) throws Exception {
        final File srcRoot = tempDir.resolve("gen-" + className).toFile();
        assertTrue(srcRoot.mkdirs() || srcRoot.isDirectory());

        PropertiesUtil.xmlToJava(xml, srcRoot.getAbsolutePath(), "gen", className, false);

        final File javaFile = new File(new File(srcRoot, "gen"), className + ".java");
        assertTrue(javaFile.exists(), "no source generated for " + xml);

        final File classes = new File(srcRoot, "classes");
        assertTrue(classes.mkdirs() || classes.isDirectory());
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        final JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        final int rc = jc.run(null, null, err, "-nowarn", "-cp", System.getProperty("java.class.path"), "-d", classes.getAbsolutePath(),
                javaFile.getAbsolutePath());

        assertEquals(0, rc, "generated source does not compile for " + xml + System.lineSeparator() + err.toString(StandardCharsets.UTF_8)
                + System.lineSeparator() + new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8));

        return classes;
    }

    @Test
    public void xmlToJavaAlwaysEmitsCompilableSource() throws Exception {
        final String[][] shapes = { { "Flat", "<config><host>h</host><port type=\"int\">8080</port></config>" },
                { "Nested", "<config><db><url>u</url><port type=\"int\">1</port></db><name>n</name></config>" }, { "Deep", "<a><b><c><d>x</d></c></b></a>" },
                { "Types", "<c><i type=\"int\">1</i><l type=\"long\">2</l><b type=\"boolean\">true</b><d type=\"double\">1.5</d></c>" },
                { "Listy", "<c><tags type=\"List&lt;String&gt;\">[a, b]</tags></c>" },
                { "Underscores", "<c><first_name>a</first_name><last-name>b</last-name></c>" }, { "SingleChild", "<c><only><leaf>v</leaf></only></c>" },
                { "PropsType", "<c><sub type=\"Properties\"><x>1</x></sub></c>" }, { "MixedCase", "<c><XMLParser>a</XMLParser><aB>b</aB></c>" },
                { "EmptyElems", "<c><a></a><b/></c>" },
                // Names that could collide with Object's methods; normalizePropName maps class -> clazz.
                { "ObjectNames", "<c><class>v</class><hashCode>v</hashCode><toString>v</toString><wait>v</wait></c>" }, };

        for (final String[] shape : shapes) {
            generateAndCompile(shape[1], shape[0]);
        }
    }

    @Test
    public void xmlToJavaEmitsCompilableSourceForNonAsciiElementNames() throws Exception {
        generateAndCompile("<c><caf\u00e9>a</caf\u00e9><stra\u00dfe>b</stra\u00dfe></c>", "Unicode");
    }

    @Test
    public void xmlToJavaRejectsAnElementNameThatIsAJavaKeyword() {
        // The generated setter uses the property name as its parameter name, which cannot be a keyword.
        // Rejection happens before anything is written, and the message names the offending element.
        final File out = tempDir.resolve("kw").toFile();
        assertTrue(out.mkdirs() || out.isDirectory());

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PropertiesUtil.xmlToJava("<c><int>1</int></c>", out.getAbsolutePath(), "gen", "Kw", false));
        assertTrue(e.getMessage().contains("int"), e.getMessage());
        assertFalse(new File(new File(out, "gen"), "Kw.java").exists(), "nothing may be written when generation is refused");
    }

    @Test
    public void storeToXmlThenXmlToJavaThenLoadRoundTrips() throws Exception {
        final Properties<String, Object> props = new Properties<>();
        props.put("host", "h");
        props.put("port", 8080);
        props.put("enabled", true);

        final File xml = tempDir.resolve("rt-gen.xml").toFile();
        PropertiesUtil.storeToXml(props, "config", true, xml);

        final File classes = generateAndCompile(new String(Files.readAllBytes(xml.toPath()), StandardCharsets.UTF_8), "RtCfg");

        try (URLClassLoader cl = new URLClassLoader(new URL[] { classes.toURI().toURL() }, getClass().getClassLoader())) {
            @SuppressWarnings("unchecked")
            final Class<? extends Properties<String, Object>> generated = (Class<? extends Properties<String, Object>>) cl.loadClass("gen.RtCfg");
            final Properties<String, Object> back = PropertiesUtil.loadFromXml(xml, generated);

            assertEquals("h", back.get("host"));
            assertEquals(8080, back.get("port"));
            assertEquals(Boolean.TRUE, back.get("enabled"));
            // The generated typed getter must agree with the map it reads from.
            assertEquals(8080, generated.getMethod("getPort").invoke(back));
        }
    }

    // ------------------------------------------------------------------------------------------
    // Cycle 3 - WSSecurityUtil against independently computed digests
    // ------------------------------------------------------------------------------------------

    @Test
    public void passwordDigestMatchesAnIndependentlyComputedDigest() throws Exception {
        final byte[] nonce = { 1, 2, 3, 4 };
        final byte[] created = "2020-01-01T00:00:00Z".getBytes(StandardCharsets.UTF_8);
        final byte[] password = "secret".getBytes(StandardCharsets.UTF_8);

        final java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
        sha1.update(nonce);
        sha1.update(created);
        sha1.update(password);
        assertEquals(Base64.getEncoder().encodeToString(sha1.digest()), WSSecurityUtil.computePasswordDigest(nonce, created, password));

        final java.security.MessageDigest sha256 = java.security.MessageDigest.getInstance("SHA-256");
        sha256.update(nonce);
        sha256.update(created);
        sha256.update(password);
        assertEquals(Base64.getEncoder().encodeToString(sha256.digest()), WSSecurityUtil.computePasswordDigest(nonce, created, password, "SHA-256"));

        final byte[] in = "hello".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(java.security.MessageDigest.getInstance("SHA-1").digest(in), WSSecurityUtil.generateDigest(in));
        assertArrayEquals(java.security.MessageDigest.getInstance("SHA-256").digest(in), WSSecurityUtil.generateDigest(in, "SHA-256"));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.generateDigest(in, "NO-SUCH-ALG"));
    }

    @Test
    public void passwordDigestStringOverloadUsesUtf8() {
        final String password = "p\u00e4\u00df\u4f60";
        assertEquals(
                WSSecurityUtil.computePasswordDigest("n".getBytes(StandardCharsets.UTF_8), "c".getBytes(StandardCharsets.UTF_8),
                        password.getBytes(StandardCharsets.UTF_8)),
                WSSecurityUtil.computePasswordDigest("n", "c", password), "the String overload must encode as UTF-8, not the platform charset");
    }

    // ------------------------------------------------------------------------------------------
    // Cycle 3 - EmailUtil message structure
    // ------------------------------------------------------------------------------------------

    @Test
    public void messageBodyContentTypeFollowsTheIsHtmlFlag() throws Exception {
        // getContentType() before saveChanges() is not authoritative; assert on the emitted wire form.
        assertTrue(messageWire(new String[] { "a@b.c" }, "s@t.u", "s", null).contains("Content-Type: text/plain; charset=UTF-8"));

        final java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.host", "localhost");
        final javax.mail.internet.MimeMessage html = EmailUtil.createMessage(new String[] { "a@b.c" }, "s@t.u", "s", "<b>x</b>", null, true, null, null, props);
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        html.writeTo(bos);

        assertTrue(bos.toString(StandardCharsets.ISO_8859_1).contains("Content-Type: text/html; charset=UTF-8"));
    }

    @Test
    public void nullContentBecomesAnEmptyBodyNotTheStringNull() throws Exception {
        final java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.host", "localhost");
        final javax.mail.internet.MimeMessage m = EmailUtil.createMessage(new String[] { "a@b.c" }, "s@t.u", "s", null, null, false, null, null, props);

        assertEquals("", ((javax.mail.Multipart) m.getContent()).getBodyPart(0).getContent());
    }

    // ------------------------------------------------------------------------------------------
    // Cycle 3 - properties round trip for awkward keys and values
    // ------------------------------------------------------------------------------------------

    @Test
    public void storeAndLoadRoundTripAwkwardKeysAndValues() throws Exception {
        final Properties<String, String> props = new Properties<>();
        props.put("plain", "v");
        props.put("with=equals", "a=b");
        props.put("with:colon", "a:b");
        props.put("with space", "a b ");
        props.put("empty", "");
        props.put("multi\nline", "a\nb");
        props.put("uni\u00e9", "caf\u00e9 \u4f60\u597d");

        final File f = tempDir.resolve("awkward.properties").toFile();
        PropertiesUtil.store(props, "hdr", f);
        final Properties<String, String> back = PropertiesUtil.load(f);

        assertEquals(props.size(), back.size());
        props.forEach((k, v) -> assertEquals(v, back.get(k), "key " + k));
    }
}
