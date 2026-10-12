package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
@Timeout(30)
class CaseSensitiveCopyJunctionReview20261003Test extends TestBase {
    @TempDir
    Path temporaryDirectory;

    @ParameterizedTest
    @CsvSource({ "false,false", "true,false", "false,true", "true,true" })
    void noncyclicJunctionWhoseTargetDiffersOnlyInCaseIsCopied(final boolean contentsOnly, final boolean resemblesOutput) throws Exception {
        enableCaseSensitivity(temporaryDirectory);
        final Path source = Files.createDirectory(temporaryDirectory.resolve("Source"));
        final Path destination = Files.createDirectory(temporaryDirectory.resolve("destination"));
        final Path output = contentsOnly ? destination : destination.resolve("Source");
        final Path external;
        if (resemblesOutput) {
            enableCaseSensitivity(destination);
            external = contentsOnly ? Files.createDirectory(temporaryDirectory.resolve("DESTINATION")) : Files.createDirectory(destination.resolve("SOURCE"));
        } else {
            external = Files.createDirectory(temporaryDirectory.resolve("source"));
        }
        Files.writeString(source.resolve("own.txt"), "source");
        Files.writeString(external.resolve("payload.txt"), "external");
        final Path link = source.resolve("junction");
        junction(link, external);
        try {
            copy(source, destination, contentsOnly);
            assertEquals("source", Files.readString(output.resolve("own.txt")));
            assertEquals("external", Files.readString(output.resolve("junction/payload.txt")));
            assertEquals("external", Files.readString(external.resolve("payload.txt")));
            assertFalse(IOUtil.isSymbolicLinkOrJunction(output.resolve("junction").toFile()));
        } finally {
            Files.deleteIfExists(link);
        }
    }

    @ParameterizedTest
    @CsvSource({ "false,source", "true,source", "false,ancestor", "true,ancestor", "false,output", "true,output" })
    void genuineSourceAncestorAndOutputCyclesAreStillOmitted(final boolean contentsOnly, final String cycle) throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "junction controls require Windows");
        final Path source = Files.createDirectory(temporaryDirectory.resolve("source"));
        final Path destination = Files.createDirectory(temporaryDirectory.resolve("destination"));
        final Path output = contentsOnly ? destination : Files.createDirectory(destination.resolve("source"));
        final Path target = switch (cycle) {
            case "source" -> source;
            case "ancestor" -> temporaryDirectory;
            default -> output;
        };
        Files.writeString(source.resolve("own.txt"), "source");
        final Path link = source.resolve("junction");
        junction(link, target);
        try {
            copy(source, destination, contentsOnly);
            assertEquals("source", Files.readString(output.resolve("own.txt")));
            assertFalse(Files.exists(output.resolve("junction")));
            assertEquals("source", Files.readString(source.resolve("own.txt")));
        } finally {
            Files.deleteIfExists(link);
        }
    }

    private static void copy(final Path source, final Path destination, final boolean contentsOnly) throws IOException {
        if (contentsOnly) {
            IOUtil.copyDirectory(source.toFile(), destination.toFile());
        } else {
            assertEquals(destination.resolve(source.getFileName()).toFile().getCanonicalFile(),
                    IOUtil.copyToDirectory(source.toFile(), destination.toFile(), false));
        }
    }

    private static void enableCaseSensitivity(final Path path) throws Exception {
        assumeTrue(IOUtil.IS_OS_WINDOWS, "case-sensitive Windows junction regression");
        command("fsutil", "file", "setCaseSensitiveInfo", path.toAbsolutePath().toString(), "enable");
    }

    private static void junction(final Path link, final Path target) throws Exception {
        command("cmd", "/c", "mklink", "/J", link.toAbsolutePath().toString(), target.toAbsolutePath().toString());
        assertTrue(Files.exists(link));
    }

    private static void command(final String... command) throws Exception {
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try {
            assumeTrue(process.waitFor(10, TimeUnit.SECONDS), "filesystem setup timed out");
            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assumeTrue(process.exitValue() == 0, "filesystem feature unavailable: " + output);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            }
        }
    }
}
