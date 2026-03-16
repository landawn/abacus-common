package com.landawn.abacus.record;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.annotation.Record;

@Record
public class Element {
    private final int id;
    private final String content;
    private final List<Long> longs;
    private final Map<String, BigInteger> map;
    private final Map<String, Double> map2;
    private final List<String> achievements;

    public Element(final int id, final String content, final List<Long> longs, final Map<String, BigInteger> map, final Map<String, Double> map2,
            final List<String> achievements) {
        this.id = id;
        this.content = content;
        this.longs = longs;
        this.map = map;
        this.map2 = map2;
        this.achievements = achievements;
    }

    public int id() {
        return id;
    }

    public String content() {
        return content;
    }

    public List<Long> longs() {
        return longs;
    }

    public Map<String, BigInteger> map() {
        return map;
    }

    public Map<String, Double> map2() {
        return map2;
    }

    public List<String> achievements() {
        return achievements;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Element other)) {
            return false;
        }

        return id == other.id && java.util.Objects.equals(content, other.content) && java.util.Objects.equals(longs, other.longs)
                && java.util.Objects.equals(map, other.map) && java.util.Objects.equals(map2, other.map2)
                && java.util.Objects.equals(achievements, other.achievements);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, content, longs, map, map2, achievements);
    }

    @Override
    public String toString() {
        return "Element{id=" + id + ", content=" + content + ", longs=" + longs + ", map=" + map + ", map2=" + map2 + ", achievements=" + achievements + "}";
    }
}
