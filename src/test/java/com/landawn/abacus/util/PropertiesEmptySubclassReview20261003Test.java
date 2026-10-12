package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class PropertiesEmptySubclassReview20261003Test extends TestBase {
    @TempDir
    Path directory;

    enum Sink {
        WRITER, STREAM, FILE
    }

    @ParameterizedTest
    @EnumSource(Sink.class)
    void emptyNestedSubclassKeepsItsStructuralType(final Sink sink) throws Exception {
        final String xml = write(new ChildProperties(), true, sink);
        assertTrue(assertInstanceOf(Properties.class, readNested(xml)).isEmpty());
        assertTrue(xml.contains("<nested type=\"Properties\">"));
    }

    @ParameterizedTest
    @EnumSource(Sink.class)
    void nullOnlyNestedSubclassAlsoNeedsTheStructuralMarker(final Sink sink) throws Exception {
        final ChildProperties nested = new ChildProperties();
        nested.put("omitted", null);
        final String xml = write(nested, true, sink);
        assertTrue(assertInstanceOf(Properties.class, readNested(xml)).isEmpty());
        assertFalse(xml.contains("omitted"));
    }

    @ParameterizedTest
    @EnumSource(Sink.class)
    void baseAndNonemptySubclassKeepTheirExistingShapes(final Sink sink) throws Exception {
        final String emptyBase = write(new Properties<>(), true, sink);
        assertTrue(emptyBase.contains("<nested type=\"Properties\">"));
        assertTrue(assertInstanceOf(Properties.class, readNested(emptyBase)).isEmpty());

        final ChildProperties nested = new ChildProperties();
        nested.put("value", "kept");
        nested.put("omitted", null);
        final String nonempty = write(nested, true, sink);
        assertTrue(nonempty.contains("<nested>"), "nonempty subclass retains nested-class generation");
        assertFalse(nonempty.contains("<nested type="));
        assertEquals("kept", assertInstanceOf(Properties.class, readNested(nonempty)).get("value"));
    }

    @ParameterizedTest
    @EnumSource(Sink.class)
    void disabledTypeInformationKeepsTheDocumentedEmptyStringFallback(final Sink sink) throws Exception {
        assertEquals("", readNested(write(new ChildProperties(), false, sink)));
    }

    @ParameterizedTest
    @EnumSource(Sink.class)
    void typedTargetStillConstructsItsDeclaredSubclass(final Sink sink) throws Exception {
        final String xml = write(new ChildProperties(), true, sink);
        final RootProperties root = PropertiesUtil.loadFromXml(new StringReader(xml), RootProperties.class);
        assertEquals(ChildProperties.class, root.getNested().getClass());
        assertTrue(root.getNested().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void generatedAccessorRepresentsAnEmptyStructuralValue(final boolean nullOnly) throws Exception {
        final ChildProperties nested = new ChildProperties();
        if (nullOnly) {
            nested.put("omitted", null);
        }
        final String xml = write(nested, true, Sink.WRITER);
        PropertiesUtil.xmlToJava(xml, directory.toString(), null, "GeneratedEmptyProperties", false);
        final Path source = directory.resolve("GeneratedEmptyProperties.java");
        final int status = ToolProvider.getSystemJavaCompiler()
                .run(null, null, null, "-proc:none", "-classpath", System.getProperty("java.class.path"), "-d", directory.toString(), source.toString());
        assertEquals(0, status);
        try (URLClassLoader loader = new URLClassLoader(new URL[] { directory.toUri().toURL() }, getClass().getClassLoader())) {
            final Class<?> generated = loader.loadClass("GeneratedEmptyProperties");
            assertEquals(Properties.class, generated.getMethod("getNested").getReturnType());
            @SuppressWarnings({ "rawtypes", "unchecked" })
            final Properties<?, ?> restored = PropertiesUtil.loadFromXml(new StringReader(xml), (Class) generated);
            assertTrue(assertInstanceOf(Properties.class, generated.getMethod("getNested").invoke(restored)).isEmpty());
        }
    }

    private String write(final Properties<String, Object> nested, final boolean typeInfo, final Sink sink) throws Exception {
        final Properties<String, Object> root = new Properties<>();
        root.put("nested", nested);
        if (sink == Sink.WRITER) {
            final StringWriter output = new StringWriter();
            PropertiesUtil.storeToXml(root, "root", typeInfo, output);
            return output.toString();
        } else if (sink == Sink.STREAM) {
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            PropertiesUtil.storeToXml(root, "root", typeInfo, output);
            return output.toString(StandardCharsets.UTF_8);
        }
        final Path output = directory.resolve("properties.xml");
        PropertiesUtil.storeToXml(root, "root", typeInfo, output.toFile());
        return Files.readString(output);
    }

    private Object readNested(final String xml) {
        return PropertiesUtil.loadFromXml(new StringReader(xml)).get("nested");
    }

    public static class ChildProperties extends Properties<String, Object> {
    }

    public static class RootProperties extends Properties<String, Object> {
        public ChildProperties getNested() {
            return (ChildProperties) get("nested");
        }

        public void setNested(final ChildProperties value) {
            put("nested", value);
        }
    }
}
