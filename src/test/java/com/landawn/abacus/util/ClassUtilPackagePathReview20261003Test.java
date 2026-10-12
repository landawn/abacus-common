package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class ClassUtilPackagePathReview20261003Test extends TestBase {
    private static Stream<Arguments> locations() {
        return Stream
                .of(Arguments.of(false, "plain"), Arguments.of(false, "spaces +#%\u00e9"), Arguments.of(false, "classes.jar!marker"),
                        Arguments.of(false, "classes.jar!"), Arguments.of(true, "plain.jar"), Arguments.of(true, "spaces +#%\u00e9.jar"),
                        Arguments.of(true, "classes.jar!marker.jar"), Arguments.of(true, "classes.jar!marker/actual.jar"),
                        Arguments.of(true, "classes.jar!/actual.jar"))
                .flatMap(args -> Stream.of((Boolean) args.get()[0] ? new String[] { "jar", "zip", "wsjar" } : new String[] { "file" })
                        .map(protocol -> Arguments.of(args.get()[0], args.get()[1], protocol)));
    }

    @ParameterizedTest(name = "archive={0}, location={1}, protocol={2}")
    @MethodSource("locations")
    void discoversClassesWithoutInterpretingPathTextAsArchiveSyntax(final boolean archive, final String location, final String protocol,
            @TempDir final Path temp) throws Exception {
        final Path classes = Files.createDirectories(temp.resolve("compiled"));
        final Path pkg = Files.createDirectories(classes.resolve("g028pathfixture"));
        final Path sub = Files.createDirectories(pkg.resolve("sub"));
        final Path keep = pkg.resolve("Keep.java");
        final Path child = sub.resolve("Child.java");
        // A scan must load metadata without executing either class's failing initializer.
        Files.writeString(keep, "package g028pathfixture; public class Keep { static { if (true) throw new AssertionError(\"initialized\"); } }");
        Files.writeString(child, "package g028pathfixture.sub; public class Child { static { if (true) throw new AssertionError(\"initialized\"); } }");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-d", classes.toString(), keep.toString(), child.toString()));

        final Path classPath = temp.resolve(location);
        if (archive) {
            Files.createDirectories(classPath.getParent());
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(classPath))) {
                for (final String entry : List.of("g028pathfixture/", "g028pathfixture/sub/", "g028pathfixture/Keep.class",
                        "g028pathfixture/sub/Child.class")) {
                    jar.putNextEntry(new JarEntry(entry));
                    if (!entry.endsWith("/")) {
                        Files.copy(classes.resolve(entry), jar);
                    }
                    jar.closeEntry();
                }
            }
        } else {
            Files.move(classes, classPath);
        }

        final List<URL> urls = new ArrayList<>();
        // Escaping literal '!' also permits a real JAR beneath a directory whose name ends in '!'.
        urls.add(archive ? URI.create(classPath.toUri().toASCIIString().replace("!", "%21")).toURL() : classPath.toUri().toURL());
        urls.add(ClassUtil.class.getProtectionDomain().getCodeSource().getLocation());
        for (final String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!entry.isEmpty()) {
                urls.add(new File(entry).toURI().toURL());
            }
        }

        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader()) {
            @Override
            public Enumeration<URL> getResources(final String name) throws java.io.IOException {
                final List<URL> resources = Collections.list(super.getResources(name));
                if (archive && !"jar".equals(protocol)) {
                    for (int i = 0; i < resources.size(); i++) {
                        final URL original = resources.get(i);
                        if ("jar".equals(original.getProtocol())) {
                            resources.set(i, new URL(null, protocol + original.toExternalForm().substring(3), new URLStreamHandler() {
                                @Override
                                protected URLConnection openConnection(final URL url) throws java.io.IOException {
                                    return original.openConnection();
                                }
                            }));
                        }
                    }
                }
                return Collections.enumeration(resources);
            }
        }) {
            assertNotNull(loader.getResource("g028pathfixture/"));
            assertEquals("g028pathfixture.Keep", Class.forName("g028pathfixture.Keep", false, loader).getName());
            final Class<?> util = Class.forName(ClassUtil.class.getName(), true, loader);
            final var scan = util.getMethod("findClassesInPackage", String.class, boolean.class, boolean.class);
            final var filteredScan = util.getMethod("findClassesInPackage", String.class, boolean.class, boolean.class, Predicate.class);

            for (final boolean recursive : new boolean[] { false, true }) {
                for (final boolean skipFailure : new boolean[] { false, true }) {
                    final List<String> expected = recursive ? List.of("g028pathfixture.Keep", "g028pathfixture.sub.Child") : List.of("g028pathfixture.Keep");
                    assertEquals(expected, names((List<?>) scan.invoke(null, "g028pathfixture", recursive, skipFailure)));
                    final List<String> visited = new ArrayList<>();
                    final Predicate<Class<?>> keepOnly = cls -> {
                        visited.add(cls.getName());
                        return cls.getSimpleName().equals("Keep");
                    };
                    assertEquals(List.of("g028pathfixture.Keep"),
                            names((List<?>) filteredScan.invoke(null, "g028pathfixture", recursive, skipFailure, keepOnly)));
                    assertEquals(expected, visited.stream().sorted().toList());
                }
            }
        }
    }

    private static List<String> names(final List<?> classes) {
        return classes.stream().map(cls -> ((Class<?>) cls).getName()).sorted().toList();
    }
}
