package com.landawn.abacus.entity.hbase;

import java.util.Objects;

import com.landawn.abacus.annotation.Type;
import com.landawn.abacus.util.HBaseColumn;

public class Name implements HbasePNL.NamePNL {
    private HBaseColumn<String> firstName;
    private HBaseColumn<String> middleName;
    private HBaseColumn<String> lastName;

    public Name() {
    }

    public Name(HBaseColumn<String> firstName, HBaseColumn<String> middleName, HBaseColumn<String> lastName) {
        this();

        setFirstName(firstName);
        setMiddleName(middleName);
        setLastName(lastName);
    }

    @Type("HBaseColumn<String>")
    public HBaseColumn<String> getFirstName() {
        return firstName;
    }

    public Name setFirstName(HBaseColumn<String> firstName) {
        this.firstName = firstName;

        return this;
    }

    public HBaseColumn<String> firstName() {
        return (HBaseColumn<String>) (this.firstName == null ? HBaseColumn.emptyOf(String.class) : firstName);
    }

    public Name setFirstName(String value) {
        setFirstName(HBaseColumn.valueOf(value));

        return this;
    }

    public Name setFirstName(String value, long version) {
        setFirstName(HBaseColumn.valueOf(value, version));

        return this;
    }

    @Type("HBaseColumn<String>")
    public HBaseColumn<String> getMiddleName() {
        return middleName;
    }

    public Name setMiddleName(HBaseColumn<String> middleName) {
        this.middleName = middleName;

        return this;
    }

    public HBaseColumn<String> middleName() {
        return (HBaseColumn<String>) (this.middleName == null ? HBaseColumn.emptyOf(String.class) : middleName);
    }

    public Name setMiddleName(String value) {
        setMiddleName(HBaseColumn.valueOf(value));

        return this;
    }

    public Name setMiddleName(String value, long version) {
        setMiddleName(HBaseColumn.valueOf(value, version));

        return this;
    }

    @Type("HBaseColumn<String>")
    public HBaseColumn<String> getLastName() {
        return lastName;
    }

    public Name setLastName(HBaseColumn<String> lastName) {
        this.lastName = lastName;

        return this;
    }

    public HBaseColumn<String> lastName() {
        return (HBaseColumn<String>) (this.lastName == null ? HBaseColumn.emptyOf(String.class) : lastName);
    }

    public Name setLastName(String value) {
        setLastName(HBaseColumn.valueOf(value));

        return this;
    }

    public Name setLastName(String value, long version) {
        setLastName(HBaseColumn.valueOf(value, version));

        return this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + Objects.hashCode(firstName);
        h = 31 * h + Objects.hashCode(middleName);
        return 31 * h + Objects.hashCode(lastName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Name other) {
            return Objects.equals(firstName, other.firstName) && Objects.equals(middleName, other.middleName) && Objects.equals(lastName, other.lastName);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{firstName=" + Objects.toString(firstName) + ", middleName=" + Objects.toString(middleName) + ", lastName=" + Objects.toString(lastName) + "}";
    }
}
