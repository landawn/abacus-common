package com.landawn.abacus.types;

import java.sql.Date;
import java.util.Objects;

import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;

public class PairTriple {

    private String str;
    private long lng;
    private Pair<String, Integer> pair;
    private Pair<String, Long> pair0;
    private Triple<String, Integer, java.util.Date> triple;
    private Tuple1<Date> tuple1;
    private Tuple2<Double, Date> tuple2;
    private Tuple2<Tuple2<Double, Date>, Date> tuple22;
    private Tuple3<Long, Double, Date> tuple3;
    private Tuple4<Long, Double, Double, Date> tuple4;
    private Tuple5<Long, Double, Double, Double, Date> tuple5;
    private Tuple6<Long, Double, Double, Double, Double, Date> tuple6;
    private Tuple7<Long, Double, Double, Double, Double, Double, Date> tuple7;

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public long getLng() {
        return lng;
    }

    public void setLng(long lng) {
        this.lng = lng;
    }

    public Pair<String, Integer> getPair() {
        return pair;
    }

    public void setPair(Pair<String, Integer> pair) {
        this.pair = pair;
    }

    public Pair<String, Long> getPair0() {
        return pair0;
    }

    public void setPair0(Pair<String, Long> pair0) {
        this.pair0 = pair0;
    }

    public Triple<String, Integer, java.util.Date> getTriple() {
        return triple;
    }

    public void setTriple(Triple<String, Integer, java.util.Date> triple) {
        this.triple = triple;
    }

    public Tuple1<Date> getTuple1() {
        return tuple1;
    }

    public void setTuple1(Tuple1<Date> tuple1) {
        this.tuple1 = tuple1;
    }

    public Tuple2<Double, Date> getTuple2() {
        return tuple2;
    }

    public void setTuple2(Tuple2<Double, Date> tuple2) {
        this.tuple2 = tuple2;
    }

    public Tuple2<Tuple2<Double, Date>, Date> getTuple22() {
        return tuple22;
    }

    public void setTuple22(Tuple2<Tuple2<Double, Date>, Date> tuple22) {
        this.tuple22 = tuple22;
    }

    public Tuple3<Long, Double, Date> getTuple3() {
        return tuple3;
    }

    public void setTuple3(Tuple3<Long, Double, Date> tuple3) {
        this.tuple3 = tuple3;
    }

    public Tuple4<Long, Double, Double, Date> getTuple4() {
        return tuple4;
    }

    public void setTuple4(Tuple4<Long, Double, Double, Date> tuple4) {
        this.tuple4 = tuple4;
    }

    public Tuple5<Long, Double, Double, Double, Date> getTuple5() {
        return tuple5;
    }

    public void setTuple5(Tuple5<Long, Double, Double, Double, Date> tuple5) {
        this.tuple5 = tuple5;
    }

    public Tuple6<Long, Double, Double, Double, Double, Date> getTuple6() {
        return tuple6;
    }

    public void setTuple6(Tuple6<Long, Double, Double, Double, Double, Date> tuple6) {
        this.tuple6 = tuple6;
    }

    public Tuple7<Long, Double, Double, Double, Double, Double, Date> getTuple7() {
        return tuple7;
    }

    public void setTuple7(Tuple7<Long, Double, Double, Double, Double, Double, Date> tuple7) {
        this.tuple7 = tuple7;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + Objects.hashCode(str);
        h = 31 * h + Objects.hashCode(lng);
        h = 31 * h + Objects.hashCode(pair);
        h = 31 * h + Objects.hashCode(pair0);
        h = 31 * h + Objects.hashCode(triple);
        h = 31 * h + Objects.hashCode(tuple1);
        h = 31 * h + Objects.hashCode(tuple2);
        h = 31 * h + Objects.hashCode(tuple22);
        h = 31 * h + Objects.hashCode(tuple3);
        h = 31 * h + Objects.hashCode(tuple4);
        h = 31 * h + Objects.hashCode(tuple5);
        h = 31 * h + Objects.hashCode(tuple6);
        return 31 * h + Objects.hashCode(tuple7);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PairTriple other) {
            return Objects.equals(str, other.str) && Objects.equals(lng, other.lng) && Objects.equals(pair, other.pair) && Objects.equals(pair0, other.pair0)
                    && Objects.equals(triple, other.triple) && Objects.equals(tuple1, other.tuple1) && Objects.equals(tuple2, other.tuple2)
                    && Objects.equals(tuple22, other.tuple22) && Objects.equals(tuple3, other.tuple3) && Objects.equals(tuple4, other.tuple4)
                    && Objects.equals(tuple5, other.tuple5) && Objects.equals(tuple6, other.tuple6) && Objects.equals(tuple7, other.tuple7);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{str=" + Objects.toString(str) + ", lng=" + Objects.toString(lng) + ", pair=" + Objects.toString(pair) + ", pair0=" + Objects.toString(pair0)
                + ", triple=" + Objects.toString(triple) + ", tuple1=" + Objects.toString(tuple1) + ", tuple2=" + Objects.toString(tuple2) + ", tuple22="
                + Objects.toString(tuple22) + ", tuple3=" + Objects.toString(tuple3) + ", tuple4=" + Objects.toString(tuple4) + ", tuple5="
                + Objects.toString(tuple5) + ", tuple6=" + Objects.toString(tuple6) + ", tuple7=" + Objects.toString(tuple7) + "}";
    }
}
