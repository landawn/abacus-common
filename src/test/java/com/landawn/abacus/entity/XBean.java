/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.entity;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Objects;

import com.landawn.abacus.parser.adapter.DateAdapter;
import com.landawn.abacus.parser.adapter.SqlDateAdapter;
import com.landawn.abacus.parser.adapter.SqlTimestampAdapter;
import com.landawn.abacus.types.WeekDay;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class XBean {
    private boolean typeBoolean;
    private Boolean typeBoolean2;
    private char typeChar;
    private byte typeByte;
    private short typeShort;
    private int typeInt;
    private long typeLong;
    private Long typeLong2;
    private float typeFloat;
    private double typeDouble;
    private String typeString;
    @XmlElement(name = "typeDate")
    @XmlJavaTypeAdapter(DateAdapter.class)
    private java.util.Date typeDate;
    @XmlElement(name = "typeSqlDate")
    @XmlJavaTypeAdapter(SqlDateAdapter.class)
    private Date typeSqlDate;
    @XmlElement(name = "typeSqlTimestamp")
    @XmlJavaTypeAdapter(SqlTimestampAdapter.class)
    private Timestamp typeSqlTimestamp;
    private WeekDay weekDay;
    private String firstName;
    private String middleName;
    private String lastName;

    public boolean getTypeBoolean() {
        return typeBoolean;
    }

    public void setTypeBoolean(boolean typeBoolean) {
        this.typeBoolean = typeBoolean;
    }

    public Boolean getTypeBoolean2() {
        return typeBoolean2;
    }

    public void setTypeBoolean2(Boolean typeBoolean2) {
        this.typeBoolean2 = typeBoolean2;
    }

    public char getTypeChar() {
        return typeChar;
    }

    public void setTypeChar(char typeChar) {
        this.typeChar = typeChar;
    }

    public byte getTypeByte() {
        return typeByte;
    }

    public void setTypeByte(byte typeByte) {
        this.typeByte = typeByte;
    }

    public short getTypeShort() {
        return typeShort;
    }

    public void setTypeShort(short typeShort) {
        this.typeShort = typeShort;
    }

    public int getTypeInt() {
        return typeInt;
    }

    public void setTypeInt(int typeInt) {
        this.typeInt = typeInt;
    }

    public long getTypeLong() {
        return typeLong;
    }

    public void setTypeLong(long typeLong) {
        this.typeLong = typeLong;
    }

    public Long getTypeLong2() {
        return typeLong2;
    }

    public void setTypeLong2(Long typeLong2) {
        this.typeLong2 = typeLong2;
    }

    public float getTypeFloat() {
        return typeFloat;
    }

    public void setTypeFloat(float typeFloat) {
        this.typeFloat = typeFloat;
    }

    public double getTypeDouble() {
        return typeDouble;
    }

    public void setTypeDouble(double typeDouble) {
        this.typeDouble = typeDouble;
    }

    public String getTypeString() {
        return typeString;
    }

    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    public java.util.Date getTypeDate() {
        return typeDate;
    }

    public void setTypeDate(java.util.Date typeDate) {
        this.typeDate = typeDate;
    }

    public Date getTypeSqlDate() {
        return typeSqlDate;
    }

    public void setTypeSqlDate(Date typeSqlDate) {
        this.typeSqlDate = typeSqlDate;
    }

    public Timestamp getTypeSqlTimestamp() {
        return typeSqlTimestamp;
    }

    public void setTypeSqlTimestamp(Timestamp typeSqlTimestamp) {
        this.typeSqlTimestamp = typeSqlTimestamp;
    }

    public WeekDay getWeekDay() {
        return weekDay;
    }

    public void setWeekDay(WeekDay weekDay) {
        this.weekDay = weekDay;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, middleName, typeBoolean, typeBoolean2, typeByte, typeChar, typeDate, typeDouble, typeFloat, typeInt, typeLong,
                typeLong2, typeShort, typeSqlDate, typeSqlTimestamp, typeString, weekDay);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        XBean other = (XBean) obj;

        if (!Objects.equals(firstName, other.firstName) || !Objects.equals(lastName, other.lastName) || !Objects.equals(middleName, other.middleName) || (typeBoolean != other.typeBoolean)) {
            return false;
        }

        if (!Objects.equals(typeBoolean2, other.typeBoolean2)) {
            return false;
        }

        if (typeByte != other.typeByte) {
            return false;
        }

        if (typeChar != other.typeChar) {
            return false;
        }

        if (!Objects.equals(typeDate, other.typeDate)) {
            return false;
        }

        if (Double.doubleToLongBits(typeDouble) != Double.doubleToLongBits(other.typeDouble)) {
            return false;
        }

        if (Float.floatToIntBits(typeFloat) != Float.floatToIntBits(other.typeFloat)) {
            return false;
        }

        if (typeInt != other.typeInt) {
            return false;
        }

        if (typeLong != other.typeLong) {
            return false;
        }

        if (!Objects.equals(typeLong2, other.typeLong2)) {
            return false;
        }

        if (typeShort != other.typeShort) {
            return false;
        }

        if (!Objects.equals(typeSqlDate, other.typeSqlDate)) {
            return false;
        }

        if (!Objects.equals(typeSqlTimestamp, other.typeSqlTimestamp)) {
            return false;
        }

        if (!Objects.equals(typeString, other.typeString)) {
            return false;
        }

        if (weekDay != other.weekDay) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "{typeBoolean=" + typeBoolean + ", typeBoolean2=" + typeBoolean2 + ", typeChar=" + typeChar + ", typeByte=" + typeByte + ", typeShort="
                + typeShort + ", typeInt=" + typeInt + ", typeLong=" + typeLong + ", typeLong2=" + typeLong2 + ", typeFloat=" + typeFloat + ", typeDouble="
                + typeDouble + ", typeString=" + typeString + ", typeDate=" + typeDate + ", typeSqlDate=" + typeSqlDate + ", typeSqlTimestamp="
                + typeSqlTimestamp + ", weekDay=" + weekDay + ", firstName=" + firstName + ", middleName=" + middleName + ", lastName=" + lastName + "}";
    }
}
