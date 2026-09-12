package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * B18: IOUtil.splitByLine must read/write as UTF-8 (or an explicitly-supplied charset) instead
 * of the JVM's platform default charset, which silently mangled non-ASCII content on
 * non-UTF-8 hosts (e.g. Windows-1252 on US Windows).
 *
 * <p>splitByLine is public API as of the 2026-08 review, so these call it directly; they used to reach
 * it by reflection when it was package-private.
 */
public class IOUtilSplitByLineCharsetTest extends TestBase {

    private static final String UTF8_CONTENT = "café\nrésumé\nnoël\nölü\n";

    @Test
    public void testSplitByLine_DefaultUsesUtf8(@TempDir final Path tmp) throws Exception {
        File source = tmp.resolve("input.txt").toFile();
        Files.writeString(source.toPath(), UTF8_CONTENT, StandardCharsets.UTF_8);

        IOUtil.splitByLine(source, 2);

        // Verify each part round-trips through UTF-8 cleanly.
        List<File> parts = listPartFiles(tmp.toFile(), "input");
        assertTrue(parts.size() >= 1, "expected at least 1 part file, got " + parts.size());
        StringBuilder reassembled = new StringBuilder();
        for (File p : parts) {
            reassembled.append(Files.readString(p.toPath(), StandardCharsets.UTF_8));
        }
        // splitByLine writes Unix line separators regardless of platform; the reassembled content
        // should contain every non-ASCII original line.
        assertTrue(reassembled.toString().contains("café"), "café missing from parts: " + reassembled);
        assertTrue(reassembled.toString().contains("résumé"), "résumé missing");
        assertTrue(reassembled.toString().contains("noël"), "noël missing");
        assertTrue(reassembled.toString().contains("ölü"), "ölü missing");
    }

    @Test
    public void testSplitByLine_ExplicitCharsetOverload(@TempDir Path tmp) throws Exception {
        // Write source as ISO-8859-1 (Latin-1) to verify the charset overload honors the request.
        File source = tmp.resolve("input.txt").toFile();
        // "café" in ISO-8859-1 is c,a,f,(0xE9). The same bytes interpreted as UTF-8 would be invalid.
        Files.write(source.toPath(), "café\nrésumé\n".getBytes(StandardCharsets.ISO_8859_1));

        IOUtil.splitByLine(source, 1, tmp.toFile(), StandardCharsets.ISO_8859_1);

        List<File> parts = listPartFiles(tmp.toFile(), "input");
        assertTrue(parts.size() >= 1);
        // Read back as ISO-8859-1; content must be intact.
        for (File p : parts) {
            String content = new String(Files.readAllBytes(p.toPath()), StandardCharsets.ISO_8859_1);
            // either part may contain "café", "résumé", or both
            assertTrue(content.contains("café") || content.contains("résumé"), "part missing expected content: " + content);
        }
    }

    @Test
    public void testSplitByLine_AsciiOnlyInputUnchanged(@TempDir Path tmp) throws Exception {
        File source = tmp.resolve("ascii.txt").toFile();
        Files.writeString(source.toPath(), "alpha\nbeta\ngamma\ndelta\n", StandardCharsets.UTF_8);

        IOUtil.splitByLine(source, 2);

        List<File> parts = listPartFiles(tmp.toFile(), "ascii");
        assertTrue(!parts.isEmpty());
        StringBuilder reassembled = new StringBuilder();
        for (File p : parts) {
            reassembled.append(Files.readString(p.toPath(), StandardCharsets.UTF_8));
        }
        assertTrue(reassembled.toString().contains("alpha"));
        assertTrue(reassembled.toString().contains("delta"));
    }

    private static List<File> listPartFiles(File dir, String prefix) {
        File[] all = dir.listFiles();
        List<File> parts = new java.util.ArrayList<>();
        if (all == null) {
            return parts;
        }
        for (File f : all) {
            String name = f.getName();
            if (name.startsWith(prefix + "_") && f.isFile()) {
                parts.add(f);
            }
        }
        return parts;
    }
}
