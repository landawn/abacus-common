package com.landawn.abacus.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class JsonHttpMessageConverterOptionalDependencyTest {
    @Test
    void scalarConversionLoadsAndRunsWithoutGuava() throws Exception {
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final List<URL> urls = new ArrayList<>();
        for (final String entry : classpath.split(Pattern.quote(File.pathSeparator))) {
            final Path path = Path.of(entry);
            final String name = path.getFileName().toString();
            if (!(name.startsWith("guava-") && name.endsWith(".jar"))) {
                urls.add(path.toUri().toURL());
            }
        }

        // An isolated loader must lack Guava itself; the normal test loader already has the optional dependency.
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName("com.google.common.collect.Multimap", false, loader));
            final Thread thread = Thread.currentThread();
            final ClassLoader originalContext = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                final Class<?> converterClass = Class.forName(JsonHttpMessageConverter.class.getName(), true, loader);
                assertSame(loader, converterClass.getClassLoader());
                final Object converter = converterClass.getConstructor().newInstance();
                final var read = converterClass.getDeclaredMethod("readInternal", Type.class, Reader.class);
                final var write = converterClass.getDeclaredMethod("writeInternal", Object.class, Type.class, Writer.class);
                read.setAccessible(true);
                write.setAccessible(true);
                final StringWriter output = new StringWriter();
                write.invoke(converter, "plain", String.class, output);
                assertEquals("\"plain\"", output.toString());
                assertEquals("plain", read.invoke(converter, String.class, new StringReader(output.toString())));
            } finally {
                thread.setContextClassLoader(originalContext);
            }
        }
    }
}
