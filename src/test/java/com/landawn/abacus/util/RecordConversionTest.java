package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

import testfixtures.record.Element;
import testfixtures.record.RecordA;
import testfixtures.record.RecordB;

/** Conversion contracts for the record-style fixtures, included in the unit suite. */
public class RecordConversionTest extends TestBase {

    static Stream<Arguments> recordConversions() {
        return Stream.of(Arguments.of(new RecordA(1, "RecordA"), RecordA.class, new RecordB(1, "RecordA"), RecordB.class),
                Arguments.of(new RecordB(2, "RecordB"), RecordB.class, new RecordA(2, "RecordB"), RecordA.class));
    }

    @ParameterizedTest
    @MethodSource("recordConversions")
    void recordRoundTripsThroughJsonAndMap(Object record, Class<?> recordType, Object converted, Class<?> convertedType) {
        Object restored = N.fromJson(N.toJson(record), recordType);
        assertEquals(record, restored);
        assertEquals(record, Beans.mapToBean(Beans.beanToMap(restored), recordType));
    }

    @ParameterizedTest
    @MethodSource("recordConversions")
    void recordConvertsToAnotherRecord(Object record, Class<?> recordType, Object converted, Class<?> convertedType) {
        assertEquals(converted, N.convert(record, convertedType));
    }

    @ParameterizedTest
    @MethodSource("recordConversions")
    void datasetConvertsEveryRecord(Object record, Class<?> recordType, Object converted, Class<?> convertedType) {
        Object restored = N.fromJson(N.toJson(record), recordType);
        Dataset dataset = N.newDataset(List.of(record, restored));
        assertEquals(List.of(converted, converted), dataset.toList(convertedType));
    }

    @Test
    void nestedRecordRoundTripsThroughMapJsonAndXml() {
        Element original = new Element(100, "abc", List.of(1L), Map.of("a", BigInteger.ZERO), Map.of("b", 2d), List.of("Speaker"));
        assertEquals(original, Beans.mapToBean(Beans.beanToMap(original), Element.class));
        assertEquals(original, N.fromJson(N.toJson(original), Element.class));
        assertEquals(original, N.fromXml(N.toXml(original), Element.class));
    }
}
