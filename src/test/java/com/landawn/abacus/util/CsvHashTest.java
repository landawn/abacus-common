package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.guava.hash.HashFunction;
import com.landawn.abacus.guava.hash.Hashing;

/**
 * Cycle 1 regression tests for the 12-class deep-review ledger
 * (scripts/cross_review/Hashing_Hex_Digest_RegEx_URLEncoded_Csv_Excel_ClassUtil_Async_CF_Futures_Profiler_ledger_2026-09-01.md).
 *
 * <p>C-026: the CsvUtil File-to-File conversions truncated the destination before converting, so any
 * failure destroyed the caller's file. C-027: the Guava hash wrappers dropped Guava's value-based
 * {@code equals}/{@code hashCode} and its descriptive {@code toString}.</p>
 */
public class CsvHashTest extends TestBase {

    @TempDir
    Path tempDir;

    private static final String PRIOR = "PREVIOUS CONTENT THAT MUST SURVIVE";

    private File withPriorContent(final String name) throws Exception {
        final File f = tempDir.resolve(name).toFile();
        Files.writeString(f.toPath(), PRIOR, StandardCharsets.UTF_8);
        return f;
    }

    private void assertUntouched(final File f) throws Exception {
        assertEquals(PRIOR, Files.readString(f.toPath(), StandardCharsets.UTF_8), "the destination must not be modified by a failed conversion");
    }

    private void assertNoTempLeftBehind() {
        final String[] leftovers = tempDir.toFile().list((dir, name) -> name.endsWith(".tmp"));
        assertEquals(0, leftovers == null ? 0 : leftovers.length, "temporary files must be cleaned up: " + Arrays.toString(leftovers));
    }

    // =============================================================================================
    // C-026 - csvToJson(File..) must not destroy the destination when the conversion fails
    // =============================================================================================

    @Test
    public void testC026_csvToJsonKeepsDestinationWhenAColumnNameIsMistyped() throws Exception {
        // The most mundane trigger there is: a typo in the selection. It is only detected after the header
        // has been read, i.e. after the destination Writer was already open (and had already truncated it).
        final File csv = tempDir.resolve("src.csv").toFile();
        Files.writeString(csv.toPath(), "id,name\n1,John\n", StandardCharsets.UTF_8);
        final File json = withPriorContent("dst.json");

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.csvToJson(csv, List.of("nmae"), json));

        assertUntouched(json);
        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_csvToJsonKeepsDestinationWhenTheSourceIsMalformed() throws Exception {
        final File csv = tempDir.resolve("bad.csv").toFile();
        // More fields than the header -> ParsingException part-way through the conversion.
        Files.writeString(csv.toPath(), "a,b\n1,2\n1,2,3,4\n", StandardCharsets.UTF_8);
        final File json = withPriorContent("dst2.json");

        assertThrows(Exception.class, () -> CsvUtil.csvToJson(csv, json));

        assertUntouched(json);
        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_jsonToCsvKeepsDestinationWhenTheSourceIsMalformed() throws Exception {
        final File json = tempDir.resolve("bad.json").toFile();
        Files.writeString(json.toPath(), "[{\"a\":1},{ NOT JSON", StandardCharsets.UTF_8);
        final File csv = withPriorContent("dst.csv");

        assertThrows(Exception.class, () -> CsvUtil.jsonToCsv(json, null, csv));

        assertUntouched(csv);
        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_converterCsvToJsonKeepsDestinationOnFailure() throws Exception {
        final File csv = tempDir.resolve("src2.csv").toFile();
        Files.writeString(csv.toPath(), "id,name\n1,John\n", StandardCharsets.UTF_8);
        final File json = withPriorContent("dst3.json");

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().source(csv).selectColumns(List.of("nope")).csvToJson(json));

        assertUntouched(json);
        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_converterJsonToCsvKeepsDestinationOnFailure() throws Exception {
        final File json = tempDir.resolve("bad2.json").toFile();
        Files.writeString(json.toPath(), "[{\"a\":1},{ NOT JSON", StandardCharsets.UTF_8);
        final File csv = withPriorContent("dst4.csv");

        assertThrows(Exception.class, () -> CsvUtil.converter().source(json).jsonToCsv(csv));

        assertUntouched(csv);
        assertNoTempLeftBehind();
    }

    // ---- the success path and the pre-existing invariants must be unchanged --------------------

    @Test
    public void testC026_successfulConversionsStillReplaceTheDestination() throws Exception {
        final File csv = tempDir.resolve("ok.csv").toFile();
        Files.writeString(csv.toPath(), "id,name\n1,John\n2,Jane\n", StandardCharsets.UTF_8);
        final File json = withPriorContent("ok.json");

        assertEquals(2, CsvUtil.csvToJson(csv, json));
        final String written = Files.readString(json.toPath(), StandardCharsets.UTF_8);
        assertTrue(written.startsWith("[\n"), written);
        assertTrue(written.contains("\"id\":\"1\""), written);

        // ...and back again.
        final File csvOut = withPriorContent("roundtrip.csv");
        assertEquals(2, CsvUtil.jsonToCsv(json, null, csvOut));
        // csvToJson emits every value as a JSON string, and the JSON object's key order is the
        // parsed map's rather than the CSV column order - so the round-trip comes back quoted and
        // reordered. Pre-existing behaviour; pinned here only to show the atomic write preserves it.
        assertEquals("\"name\",\"id\"\n\"John\",\"1\"\n\"Jane\",\"2\"", Files.readString(csvOut.toPath(), StandardCharsets.UTF_8));
        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_noSourceStillThrowsWithoutCreatingTheDestination() {
        // Locked by CsvUtilTest#testCsvConverterCsvToJsonThrowsWithoutSource / ...JsonToCsv...: the
        // validation must happen before anything is opened, so no destination and no temp file appear.
        final File json = tempDir.resolve("never-created.json").toFile();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().csvToJson(json));
        assertFalse(json.exists(), "a missing source must not create the destination");

        final File csv = tempDir.resolve("never-created.csv").toFile();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.converter().jsonToCsv(csv));
        assertFalse(csv.exists(), "a missing source must not create the destination");

        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_missingSourceFileLeavesTheDestinationAlone() throws Exception {
        final File missing = tempDir.resolve("no-such-source.csv").toFile();
        final File json = withPriorContent("dst5.json");

        assertThrows(Exception.class, () -> CsvUtil.csvToJson(missing, json));

        assertUntouched(json);
        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_emptyAndUnicodeSourcesRoundTrip() throws Exception {
        // Empty JSON array: an explicit header selection is still written (see C-004 in the ledger).
        final File emptyJson = tempDir.resolve("empty.json").toFile();
        Files.writeString(emptyJson.toPath(), "[]", StandardCharsets.UTF_8);
        final File csv = tempDir.resolve("empty.csv").toFile();
        assertEquals(0, CsvUtil.jsonToCsv(emptyJson, Arrays.asList("a", "b"), csv));
        assertEquals("\"a\",\"b\"", Files.readString(csv.toPath(), StandardCharsets.UTF_8));

        // Non-ASCII must survive the temp-file hop: the temp file is written with the same charset the
        // destination used to be written with (IOUtil.DEFAULT_CHARSET = UTF-8).
        final File uniJson = tempDir.resolve("uni.json").toFile();
        Files.writeString(uniJson.toPath(), "[{\"k\":\"中文☃\"}]", StandardCharsets.UTF_8);
        final File uniCsv = tempDir.resolve("uni.csv").toFile();
        assertEquals(1, CsvUtil.jsonToCsv(uniJson, null, uniCsv));
        assertEquals("\"k\"\n\"中文☃\"", Files.readString(uniCsv.toPath(), StandardCharsets.UTF_8));

        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_shortDestinationNameIsAccepted() throws Exception {
        // File.createTempFile rejects a prefix shorter than three characters, which a destination named "a"
        // would otherwise produce.
        final File json = tempDir.resolve("s.json").toFile();
        final File csv = tempDir.resolve("a").toFile();
        Files.writeString(json.toPath(), "[{\"x\":1}]", StandardCharsets.UTF_8);

        assertEquals(1, CsvUtil.jsonToCsv(json, null, csv));
        assertEquals("\"x\"\n1", Files.readString(csv.toPath(), StandardCharsets.UTF_8));
        assertNoTempLeftBehind();
    }

    @Test
    public void testC026_writerOverloadsAreUnaffected() {
        // The Writer overloads take a caller-owned sink with no File to move onto; they must keep writing
        // straight through, including the partial output produced before a mid-stream failure.
        final StringWriter out = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.csvToJson(new StringReader("id,name\n1,John"), List.of("nmae"), out, null));

        final StringWriter ok = new StringWriter();
        assertEquals(1, CsvUtil.csvToJson(new StringReader("id,name\n1,John"), null, ok, null));
        assertTrue(ok.toString().contains("\"id\":\"1\""), ok.toString());
    }

    // =============================================================================================
    // C-027 - the Guava HashFunction wrapper must not drop Guava's identity semantics
    // =============================================================================================

    @Test
    public void testC027_hashFunctionToStringNamesTheAlgorithm() {
        assertEquals("Hashing.murmur3_128(42)", Hashing.murmur3_128(42).toString());
        assertEquals("Hashing.sha256()", Hashing.sha256().toString());
        assertEquals("Hashing.crc32c()", Hashing.crc32c().toString());
        // Guava's fixed variant still describes itself as murmur3_32.
        assertEquals("Hashing.murmur3_32(0)", Hashing.murmur3_32().toString());
    }

    @Test
    public void testC027_hashFunctionEqualityIsValueBasedLikeGuava() {
        final HashFunction a = Hashing.murmur3_128(42);
        final HashFunction b = Hashing.murmur3_128(42);

        assertNotEquals(System.identityHashCode(a), System.identityHashCode(b), "two distinct wrapper instances");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // Reflexive, and not equal across algorithms or seeds.
        assertEquals(a, a);
        assertNotEquals(a, Hashing.murmur3_128(43));
        assertNotEquals(a, Hashing.sha256());
        assertNotEquals(a, Hashing.murmur3_32(42));
    }

    @Test
    public void testC027_hashFunctionEqualsRejectsNullAndForeignTypes() {
        final HashFunction a = Hashing.sha256();

        assertNotEquals(null, a);
        assertNotEquals("Hashing.sha256()", a);
        assertNotEquals(a, com.google.common.hash.Hashing.sha256(), "an unwrapped Guava function is not a wrapper");
    }

    @Test
    public void testC027_cachedFactoriesRemainConsistent() {
        // The no-arg factories return a cached instance; equality must agree with that, not fight it.
        assertEquals(Hashing.sha256(), Hashing.sha256());
        assertEquals(Hashing.sha256().hashCode(), Hashing.sha256().hashCode());
        assertEquals(Hashing.murmur3_128(), Hashing.murmur3_128(0));
    }

    @Test
    public void testC027_hashingStillHashes() {
        // Guard: adding equality must not disturb the actual hashing.
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", Hashing.md5().hash("hello world".getBytes(StandardCharsets.UTF_8)).toString());
        assertEquals(Hashing.murmur3_128(42).hash(new byte[] { 1, 2, 3 }), Hashing.murmur3_128(42).hash(new byte[] { 1, 2, 3 }));
        assertEquals(128, Hashing.murmur3_128(42).bits());
    }

    @Test
    public void testC027_hasherKeepsIdentitySemantics() {
        // A Hasher is a mutable, single-use accumulator, and Guava's own Hashers use identity equality; the
        // wrapper must not invent value equality for it.
        final com.landawn.abacus.guava.hash.Hasher h = Hashing.sha256().newHasher();
        assertEquals(h, h);
        assertNotEquals(h, Hashing.sha256().newHasher());
    }
}
