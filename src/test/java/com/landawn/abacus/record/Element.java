package com.landawn.abacus.record;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.Record;

import lombok.Value;
import lombok.experimental.Accessors;

@Record
@Value
@Accessors(fluent = true)
public class Element {
    private final int id;
    private final String content;
    private final List<Long> longs;
    private final Map<String, BigInteger> map;
    private final Map<String, Double> map2;
    private final List<String> achievements;
}