package com.landawn.abacus.util;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.landawn.abacus.annotation.Type;

public class Entity_1 {

    private String gui;
    private String firstName;
    private String lastName;
    private Date birthDate;
    private java.sql.Date birthDate2;
    private Timestamp createdTime;
    @Type("Map<List<com.landawn.abacus.util.stream.Stream<String>>, String>")
    private Map<List<com.landawn.abacus.util.stream.Stream<String>>, String> map;

    public Entity_1() {
    }

    public Entity_1(String gui, String firstName, String lastName, Date birthDate, java.sql.Date birthDate2, Timestamp createdTime,
            Map<List<com.landawn.abacus.util.stream.Stream<String>>, String> map) {
        this.gui = gui;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.birthDate2 = birthDate2;
        this.createdTime = createdTime;
        this.map = map;
    }

    public String getGUI() {
        return gui;
    }

    public Entity_1 setGUI(String gui) {
        this.gui = gui;

        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Entity_1 setFirstName(String firstName) {
        this.firstName = firstName;

        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Entity_1 setLastName(String lastName) {
        this.lastName = lastName;

        return this;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public Entity_1 setBirthDate(Date birthDate) {
        this.birthDate = birthDate;

        return this;
    }

    public java.sql.Date getBirthDate2() {
        return birthDate2;
    }

    public Entity_1 setBirthDate2(java.sql.Date birthDate2) {
        this.birthDate2 = birthDate2;

        return this;
    }

    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public Entity_1 setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;

        return this;
    }

    @Type("Map<List<com.landawn.abacus.util.stream.Stream<String>>, String>")
    public Map<List<com.landawn.abacus.util.stream.Stream<String>>, String> getMap() {
        return map;
    }

    public Entity_1 setMap(Map<List<com.landawn.abacus.util.stream.Stream<String>>, String> map) {
        this.map = map;

        return this;
    }

    public Entity_1 copy() {
        final Entity_1 copy = new Entity_1();

        copy.gui = this.gui;
        copy.firstName = this.firstName;
        copy.lastName = this.lastName;
        copy.birthDate = this.birthDate;
        copy.birthDate2 = this.birthDate2;
        copy.createdTime = this.createdTime;
        copy.map = this.map;

        return copy;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + Objects.hashCode(gui);
        h = 31 * h + Objects.hashCode(firstName);
        h = 31 * h + Objects.hashCode(lastName);
        h = 31 * h + Objects.hashCode(birthDate);
        h = 31 * h + Objects.hashCode(birthDate2);
        h = 31 * h + Objects.hashCode(createdTime);
        return 31 * h + Objects.hashCode(map);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Entity_1 other) {
            return Objects.equals(gui, other.gui) && Objects.equals(firstName, other.firstName) && Objects.equals(lastName, other.lastName)
                    && Objects.equals(birthDate, other.birthDate) && Objects.equals(birthDate2, other.birthDate2)
                    && Objects.equals(createdTime, other.createdTime) && Objects.equals(map, other.map);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{gui=" + Objects.toString(gui) + ", firstName=" + Objects.toString(firstName) + ", lastName=" + Objects.toString(lastName) + ", birthDate="
                + Objects.toString(birthDate) + ", birthDate2=" + Objects.toString(birthDate2) + ", createdTime=" + Objects.toString(createdTime) + ", map="
                + Objects.toString(map) + "}";
    }
}
