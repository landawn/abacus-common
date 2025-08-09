/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.parser.entity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TypeBean {
    private boolean boolType;
    private boolean[] boolArrayType;
    private List<Boolean> booleanListType;

    private char charType = '0';
    private char[] charArrayType;
    private List<Character> characterListType;

    private byte byteType;
    private byte[] byteArrayType;
    private List<Byte> byteListType;

    private short shortType;
    private short[] shortArrayType;
    private List<Short> shortListType;

    private int intType;
    private int[] intArrayType;
    private List<Integer> intListType;

    private long longType;
    private long[] longArrayType;
    private List<Long> longListType;

    private float floatType;
    private float[] floatArrayType;
    private List<Float> floatListType;

    private double doubleType;
    private double[] doubleArrayType;
    private List<Double> doubleListType;

    private String StringType;
    private String[] StringArrayType;
    private List<String> StringListType;

    private Date dateType;
    private Date[] dateArrayType;
    private List<Date> dateListType;

    public boolean isBoolType() {
        return boolType;
    }

    public void setBoolType(boolean boolType) {
        this.boolType = boolType;
    }

    public boolean[] getBoolArrayType() {
        return boolArrayType;
    }

    public void setBoolArrayType(boolean[] boolArrayType) {
        this.boolArrayType = boolArrayType;
    }

    public List<Boolean> getBooleanListType() {
        return booleanListType;
    }

    public void setBooleanListType(List<Boolean> booleanListType) {
        this.booleanListType = booleanListType;
    }

    public char getCharType() {
        return charType;
    }

    public void setCharType(char charType) {
        this.charType = charType;
    }

    public char[] getCharArrayType() {
        return charArrayType;
    }

    public void setCharArrayType(char[] charArrayType) {
        this.charArrayType = charArrayType;
    }

    public List<Character> getCharacterListType() {
        return characterListType;
    }

    public void setCharacterListType(List<Character> characterListType) {
        this.characterListType = characterListType;
    }

    public byte getByteType() {
        return byteType;
    }

    public void setByteType(byte byteType) {
        this.byteType = byteType;
    }

    public byte[] getByteArrayType() {
        return byteArrayType;
    }

    public void setByteArrayType(byte[] byteArrayType) {
        this.byteArrayType = byteArrayType;
    }

    public List<Byte> getByteListType() {
        return byteListType;
    }

    public void setByteListType(List<Byte> byteListType) {
        this.byteListType = byteListType;
    }

    public short getShortType() {
        return shortType;
    }

    public void setShortType(short shortType) {
        this.shortType = shortType;
    }

    public short[] getShortArrayType() {
        return shortArrayType;
    }

    public void setShortArrayType(short[] shortArrayType) {
        this.shortArrayType = shortArrayType;
    }

    public List<Short> getShortListType() {
        return shortListType;
    }

    public void setShortListType(List<Short> shortListType) {
        this.shortListType = shortListType;
    }

    public int getIntType() {
        return intType;
    }

    public void setIntType(int intType) {
        this.intType = intType;
    }

    public int[] getIntArrayType() {
        return intArrayType;
    }

    public void setIntArrayType(int[] intArrayType) {
        this.intArrayType = intArrayType;
    }

    public List<Integer> getIntListType() {
        return intListType;
    }

    public void setIntListType(List<Integer> intListType) {
        this.intListType = intListType;
    }

    public long getLongType() {
        return longType;
    }

    public void setLongType(long longType) {
        this.longType = longType;
    }

    public long[] getLongArrayType() {
        return longArrayType;
    }

    public void setLongArrayType(long[] longArrayType) {
        this.longArrayType = longArrayType;
    }

    public List<Long> getLongListType() {
        return longListType;
    }

    public void setLongListType(List<Long> longListType) {
        this.longListType = longListType;
    }

    public float getFloatType() {
        return floatType;
    }

    public void setFloatType(float floatType) {
        this.floatType = floatType;
    }

    public float[] getFloatArrayType() {
        return floatArrayType;
    }

    public void setFloatArrayType(float[] floatArrayType) {
        this.floatArrayType = floatArrayType;
    }

    public List<Float> getFloatListType() {
        return floatListType;
    }

    public void setFloatListType(List<Float> floatListType) {
        this.floatListType = floatListType;
    }

    public double getDoubleType() {
        return doubleType;
    }

    public void setDoubleType(double doubleType) {
        this.doubleType = doubleType;
    }

    public double[] getDoubleArrayType() {
        return doubleArrayType;
    }

    public void setDoubleArrayType(double[] doubleArrayType) {
        this.doubleArrayType = doubleArrayType;
    }

    public List<Double> getDoubleListType() {
        return doubleListType;
    }

    public void setDoubleListType(List<Double> doubleListType) {
        this.doubleListType = doubleListType;
    }

    public String getStringType() {
        return StringType;
    }

    public void setStringType(String stringType) {
        StringType = stringType;
    }

    public String[] getStringArrayType() {
        return StringArrayType;
    }

    public void setStringArrayType(String[] stringArrayType) {
        StringArrayType = stringArrayType;
    }

    public List<String> getStringListType() {
        return StringListType;
    }

    public void setStringListType(List<String> stringListType) {
        StringListType = stringListType;
    }

    public Date getDateType() {
        return dateType;
    }

    public void setDateType(Date dateType) {
        this.dateType = dateType;
    }

    public Date[] getDateArrayType() {
        return dateArrayType;
    }

    public void setDateArrayType(Date[] dateArrayType) {
        this.dateArrayType = dateArrayType;
    }

    public List<Date> getDateListType() {
        return dateListType;
    }

    public void setDateListType(List<Date> dateListType) {
        this.dateListType = dateListType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(StringArrayType), StringListType, StringType, Arrays.hashCode(boolArrayType), boolType, booleanListType,
                Arrays.hashCode(byteArrayType), byteListType, byteType, Arrays.hashCode(charArrayType), charType, characterListType,
                Arrays.hashCode(dateArrayType), dateListType, dateType, Arrays.hashCode(doubleArrayType), doubleListType, doubleType,
                Arrays.hashCode(floatArrayType), floatListType, floatType, Arrays.hashCode(intArrayType), intListType, intType, Arrays.hashCode(longArrayType),
                longListType, longType, Arrays.hashCode(shortArrayType), shortListType, shortType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        TypeBean other = (TypeBean) obj;
        if (!Arrays.equals(StringArrayType, other.StringArrayType)) {
            return false;
        }
        if (!Objects.equals(StringListType, other.StringListType)) {
            return false;
        }
        if (!Objects.equals(StringType, other.StringType)) {
            return false;
        }
        if (!Arrays.equals(boolArrayType, other.boolArrayType)) {
            return false;
        }
        if (boolType != other.boolType) {
            return false;
        }
        if (!Objects.equals(booleanListType, other.booleanListType)) {
            return false;
        }
        if (!Arrays.equals(byteArrayType, other.byteArrayType)) {
            return false;
        }
        if (!Objects.equals(byteListType, other.byteListType)) {
            return false;
        }
        if (byteType != other.byteType) {
            return false;
        }
        if (!Arrays.equals(charArrayType, other.charArrayType)) {
            return false;
        }
        if (charType != other.charType) {
            return false;
        }
        if (!Objects.equals(characterListType, other.characterListType)) {
            return false;
        }
        if (!Arrays.equals(dateArrayType, other.dateArrayType)) {
            return false;
        }
        if (!Objects.equals(dateListType, other.dateListType)) {
            return false;
        }
        if (!Objects.equals(dateType, other.dateType)) {
            return false;
        }
        if (!Arrays.equals(doubleArrayType, other.doubleArrayType)) {
            return false;
        }
        if (!Objects.equals(doubleListType, other.doubleListType)) {
            return false;
        }
        if (Double.doubleToLongBits(doubleType) != Double.doubleToLongBits(other.doubleType)) {
            return false;
        }
        if (!Arrays.equals(floatArrayType, other.floatArrayType)) {
            return false;
        }
        if (!Objects.equals(floatListType, other.floatListType)) {
            return false;
        }
        if (Float.floatToIntBits(floatType) != Float.floatToIntBits(other.floatType)) {
            return false;
        }
        if (!Arrays.equals(intArrayType, other.intArrayType)) {
            return false;
        }
        if (!Objects.equals(intListType, other.intListType)) {
            return false;
        }
        if (intType != other.intType) {
            return false;
        }
        if (!Arrays.equals(longArrayType, other.longArrayType)) {
            return false;
        }
        if (!Objects.equals(longListType, other.longListType)) {
            return false;
        }
        if (longType != other.longType) {
            return false;
        }
        if (!Arrays.equals(shortArrayType, other.shortArrayType)) {
            return false;
        }
        if (!Objects.equals(shortListType, other.shortListType)) {
            return false;
        }
        if (shortType != other.shortType) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "{boolType=" + boolType + ", boolArrayType=" + Arrays.toString(boolArrayType) + ", booleanListType=" + booleanListType + ", charType=" + charType
                + ", charArrayType=" + Arrays.toString(charArrayType) + ", characterListType=" + characterListType + ", byteType=" + byteType
                + ", byteArrayType=" + Arrays.toString(byteArrayType) + ", byteListType=" + byteListType + ", shortType=" + shortType + ", shortArrayType="
                + Arrays.toString(shortArrayType) + ", shortListType=" + shortListType + ", intType=" + intType + ", intArrayType="
                + Arrays.toString(intArrayType) + ", intListType=" + intListType + ", longType=" + longType + ", longArrayType="
                + Arrays.toString(longArrayType) + ", longListType=" + longListType + ", floatType=" + floatType + ", floatArrayType="
                + Arrays.toString(floatArrayType) + ", floatListType=" + floatListType + ", doubleType=" + doubleType + ", doubleArrayType="
                + Arrays.toString(doubleArrayType) + ", doubleListType=" + doubleListType + ", StringType=" + StringType + ", StringArrayType="
                + Arrays.toString(StringArrayType) + ", StringListType=" + StringListType + ", dateType=" + dateType + ", dateArrayType="
                + Arrays.toString(dateArrayType) + ", dateListType=" + dateListType + "}";
    }

}
