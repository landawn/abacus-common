package com.landawn.abacus.parser.entity;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class XBean {
    @XmlElement(name = "typeBoolean")
    private boolean typeBoolean;
    @XmlElement(name = "typeBoolean2")
    private Boolean typeBoolean2;
    @XmlElement(name = "typeChar")
    private char typeChar;
    @XmlElement(name = "typeByte")
    private byte typeByte;
    @XmlElement(name = "typeShort")
    private short typeShort;
    @XmlElement(name = "typeInt")
    private int typeInt;
    @XmlElement(name = "typeLong")
    private long typeLong;
    @XmlElement(name = "typeLong2")
    private Long typeLong2;
    @XmlElement(name = "typeFloat")
    private float typeFloat;
    @XmlElement(name = "typeDouble")
    private double typeDouble;
    @XmlElement(name = "typeString")
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
    @XmlElement(name = "weekDay")
    private WeekDay weekDay;
    @XmlElement(name = "firstName")
    private String firstName;
    @XmlElement(name = "middleName")
    private String middleName;
    @XmlElement(name = "lastName")
    private String lastName;
    @XmlElement(name = "array")
    private String[] array;
    @XmlElement(required = true)
    private List<PersonType> persons;

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

    public String[] getArray() {
        return array;
    }

    public void setArray(String[] array) {
        this.array = array;
    }

    public List<PersonType> getPersons() {
        if (persons == null) {
            persons = new ArrayList<>();
        }

        return this.persons;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(array), firstName, lastName, middleName, persons, typeBoolean, typeBoolean2, typeByte, typeChar, typeDate,
                typeDouble, typeFloat, typeInt, typeLong, typeLong2, typeShort, typeSqlDate, typeSqlTimestamp, typeString, weekDay);
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

        if (!Arrays.equals(array, other.array)) {
            return false;
        }

        if (!Objects.equals(firstName, other.firstName)) {
            return false;
        }

        if (!Objects.equals(lastName, other.lastName)) {
            return false;
        }

        if (!Objects.equals(middleName, other.middleName)) {
            return false;
        }

        if (!Objects.equals(persons, other.persons)) {
            return false;
        }

        if (typeBoolean != other.typeBoolean) {
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
                + typeSqlTimestamp + ", weekDay=" + weekDay + ", firstName=" + firstName + ", middleName=" + middleName + ", lastName=" + lastName + ", array="
                + Arrays.toString(array) + ", persons=" + persons + "}";
    }
}
