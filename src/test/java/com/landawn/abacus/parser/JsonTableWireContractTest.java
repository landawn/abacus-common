package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.Sheet;

/** Exercise the table wire formats at the parser boundary, including nested values and malformed metadata. */
class JsonTableWireContractTest extends TestBase {
    private final JsonParser parser = ParserFactory.createJsonParser();

    static java.util.stream.Stream<Arguments> formats() {
        return java.util.stream.IntStream.range(0, 16).mapToObj(bits -> Arguments.of((bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0));
    }

    @ParameterizedTest(name = "pretty={0}, quoted={1}, types={2}, nested={3}")
    @MethodSource("formats")
    void datasetRoundTripPreservesColumnsPropertiesAndFrozenState(boolean pretty, boolean quoted, boolean types, boolean nested) {
        Dataset expected = Dataset.rows(List.of("id", "name"), new Object[][] { { 1, "alpha" }, { 2, "beta" } });
        expected.setProperties(Map.of("owner", "team", "revision", 3));
        expected.freeze();
        JsonSerConfig config = config(pretty, quoted, types);
        String json = parser.serialize(nested ? Map.of("table", expected) : expected, config);
        Dataset actual = nested ? parser.deserialize(new StringReader(json), Type.<Map<String, Dataset>> of("Map<String, Dataset>")).get("table")
                : parser.deserialize(new StringReader(json), Dataset.class);
        assertEquals(expected, actual);
        assertEquals(expected.getProperties(), actual.getProperties());
        assertTrue(actual.isFrozen());
        assertThrows(IllegalStateException.class, () -> actual.set(0, 0, 99));
    }

    @ParameterizedTest(name = "pretty={0}, quoted={1}, types={2}, nested={3}")
    @MethodSource("formats")
    void sheetRoundTripPreservesKeysCellsAndFrozenState(boolean pretty, boolean quoted, boolean types, boolean nested) {
        Sheet<String, String, Integer> expected = Sheet.rows(List.of("r1", "r2"), List.of("a", "b"), new Integer[][] { { 1, 2 }, { 3, 4 } });
        expected.freeze();
        JsonSerConfig config = config(pretty, quoted, types).setWriteRowColumnKeyType(types);
        String json = parser.serialize(nested ? Map.of("table", expected) : expected, config);
        Sheet<String, String, Integer> actual = nested
                ? parser.deserialize(new StringReader(json),
                        Type.<Map<String, Sheet<String, String, Integer>>> of("Map<String, Sheet<String, String, Integer>>")).get("table")
                : parser.deserialize(new StringReader(json), Type.of("Sheet<String, String, Integer>"));
        assertEquals(expected, actual);
        assertTrue(actual.isFrozen());
        assertEquals(4, actual.get("r2", "b"));
    }

    @ParameterizedTest(name = "pretty={0}, quoted={1}, types={2}, nested={3}")
    @MethodSource("formats")
    void entityRoundTripPreservesNamedProperties(boolean pretty, boolean quoted, boolean types, boolean nested) {
        MapEntity entity = new MapEntity("Record").set("id", 7).set("name", "alpha");
        Seid id = Seid.of("Record").set("id", 7).set("region", "west");
        JsonSerConfig config = config(pretty, quoted, types);
        String entityJson = parser.serialize(nested ? Map.of("entity", entity) : entity, config);
        MapEntity actualEntity = nested
                ? parser.deserialize(new StringReader(entityJson), Type.<Map<String, MapEntity>> of("Map<String, MapEntity>")).get("entity")
                : parser.deserialize(new StringReader(entityJson), MapEntity.class);
        assertEquals(entity, actualEntity);
        String idJson = parser.serialize(nested ? Map.of("id", id) : id, config);
        Seid actualId = nested
                ? parser.deserialize(new StringReader(idJson), Type.<Map<String, Seid>> of("Map<String, com.landawn.abacus.util.Seid>")).get("id")
                : parser.deserialize(new StringReader(idJson), Seid.class);
        assertEquals(id, actualId);
    }

    private static JsonSerConfig config(boolean pretty, boolean quoted, boolean types) {
        return JsonSerConfig.create().setPrettyFormat(pretty).setIndentation("  ").setQuotePropName(quoted).setWriteColumnType(types);
    }

    static java.util.stream.Stream<Arguments> invalidDocuments() {
        java.util.stream.Stream<Arguments> datasets = java.util.stream.Stream
                .of("[1]", "[{},1]", "[{a:1", "[{a:{x:1} b:2}]", "[{a:[1] b:2}]", "{unknown:'x'}", "{unknown:1,isFrozen:false}", "{unknown:[]}", "{unknown:{}}",
                        "{columnNames:'a'}", "{columnNames:1,isFrozen:false}", "{isFrozen:[]}", "{properties:[]}", "{columnNames:['a'],columns:{a:'x'}}",
                        "{columnNames:['a'],columns:{a:1}}", "{columnNames:['a'],columns:{a:{}}}", "{columnNames:['a'],columns:{a:1,b:[2]}}",
                        "{columnNames:['a'],columns:{a:[1]},columnNames:['a']}", "{columnNames:['a','b'],columns:{a:[1],b:[2,3]}}")
                .map(json -> Arguments.of(Dataset.class, json));
        java.util.stream.Stream<Arguments> sheets = java.util.stream.Stream
                .of("{unknown:'x'}", "{unknown:1,isFrozen:false}", "{unknown:[]}", "{unknown:{}}", "{rowKeySet:'r'}", "{columnKeySet:'a'}",
                        "{rowKeySet:1,isFrozen:false}", "{isFrozen:[]}", "{rowKeySet:['r'],columnKeySet:['a'],columns:{a:'x'}}",
                        "{rowKeySet:['r'],columnKeySet:['a'],columns:{a:1}}", "{rowKeySet:['r'],columnKeySet:['a'],columns:{a:{}}}",
                        "{rowKeySet:['r'],columnKeySet:['a'],columns:{a:1,b:[2]}}",
                        "{rowKeySet:['r'],columnKeySet:['a'],columns:{a:[1]},columnTypes:['Integer']}",
                        "{rowKeySet:['r'],columnKeySet:['a'],columns:{a:[1]},columnKeySet:['a']}")
                .map(json -> Arguments.of(Sheet.class, json));
        return java.util.stream.Stream.concat(datasets, sheets);
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidDocuments")
    void malformedTableMetadataIsRejectedForBothReaders(Class<?> target, String json) {
        assertThrows(ParsingException.class, () -> parser.deserialize(json, target));
        assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(json), target));
    }
}
