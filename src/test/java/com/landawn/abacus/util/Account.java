/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.sql.Timestamp;
import java.util.List;

import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.util.entity.AccountContact;
import com.landawn.abacus.util.entity.AccountDevice;
import com.landawn.abacus.util.entity.ExtendDirtyBasicPNL;

public class Account implements ExtendDirtyBasicPNL.AccountPNL {
    private String id;
    private String gui;
    private String emailAddress;
    private String firstName;
    private String middleName;
    private String lastName;
    private Timestamp birthDate;
    private int status;
    @DiffIgnore
    private Timestamp lastUpdateTime;
    private Timestamp createdTime;
    private AccountContact contact;
    private List<AccountDevice> devices;

    public Account() {
    }

    public Account(String id) {
        this();

        setId(id);
    }

    public Account(String gui, String emailAddress, String firstName, String middleName, String lastName, Timestamp birthDate, int status,
            Timestamp lastUpdateTime, Timestamp createdTime, AccountContact contact, List<AccountDevice> devices) {
        this();

        setGUI(gui);
        setEmailAddress(emailAddress);
        setFirstName(firstName);
        setMiddleName(middleName);
        setLastName(lastName);
        setBirthDate(birthDate);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreatedTime(createdTime);
        setContact(contact);
        setDevices(devices);
    }

    public Account(String id, String gui, String emailAddress, String firstName, String middleName, String lastName, Timestamp birthDate, int status,
            Timestamp lastUpdateTime, Timestamp createdTime, AccountContact contact, List<AccountDevice> devices) {
        this();

        setId(id);
        setGUI(gui);
        setEmailAddress(emailAddress);
        setFirstName(firstName);
        setMiddleName(middleName);
        setLastName(lastName);
        setBirthDate(birthDate);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreatedTime(createdTime);
        setContact(contact);
        setDevices(devices);
    }

    public String getId() {
        return id;
    }

    public Account setId(String id) {
        this.id = id;

        return this;
    }

    public String getGUI() {
        return gui;
    }

    public Account setGUI(String gui) {
        this.gui = gui;

        return this;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public Account setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;

        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public Account setFirstName(String firstName) {
        this.firstName = firstName;

        return this;
    }

    public String getMiddleName() {
        return middleName;
    }

    public Account setMiddleName(String middleName) {
        this.middleName = middleName;

        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public Account setLastName(String lastName) {
        this.lastName = lastName;

        return this;
    }

    public Timestamp getBirthDate() {
        return birthDate;
    }

    public Account setBirthDate(Timestamp birthDate) {
        this.birthDate = birthDate;

        return this;
    }

    public int getStatus() {
        return status;
    }

    public Account setStatus(int status) {
        this.status = status;

        return this;
    }

    public Timestamp getLastUpdateTime() {
        return lastUpdateTime;
    }

    public Account setLastUpdateTime(Timestamp lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;

        return this;
    }

    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public Account setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;

        return this;
    }

    public AccountContact getContact() {
        return contact;
    }

    public Account setContact(AccountContact contact) {
        this.contact = contact;

        return this;
    }

    public List<AccountDevice> getDevices() {
        return devices;
    }

    public Account setDevices(List<AccountDevice> devices) {
        this.devices = devices;

        return this;
    }

    public Account copy() {
        Account copy = new Account();

        copy.id = this.id;
        copy.gui = this.gui;
        copy.emailAddress = this.emailAddress;
        copy.firstName = this.firstName;
        copy.middleName = this.middleName;
        copy.lastName = this.lastName;
        copy.birthDate = this.birthDate;
        copy.status = this.status;
        copy.lastUpdateTime = this.lastUpdateTime;
        copy.createdTime = this.createdTime;
        copy.contact = this.contact;
        copy.devices = this.devices;

        return copy;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(id);
        h = 31 * h + N.hashCode(gui);
        h = 31 * h + N.hashCode(emailAddress);
        h = 31 * h + N.hashCode(firstName);
        h = 31 * h + N.hashCode(middleName);
        h = 31 * h + N.hashCode(lastName);
        h = 31 * h + N.hashCode(birthDate);
        h = 31 * h + N.hashCode(status);
        h = 31 * h + N.hashCode(lastUpdateTime);
        h = 31 * h + N.hashCode(createdTime);
        h = 31 * h + N.hashCode(contact);
        return 31 * h + N.hashCode(devices);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Account other) {
            if (N.equals(id, other.id) && N.equals(gui, other.gui) && N.equals(emailAddress, other.emailAddress) && N.equals(firstName, other.firstName)
                    && N.equals(middleName, other.middleName) && N.equals(lastName, other.lastName) && N.equals(birthDate, other.birthDate)
                    && N.equals(status, other.status) && N.equals(lastUpdateTime, other.lastUpdateTime) && N.equals(createdTime, other.createdTime)
                    && N.equals(contact, other.contact) && N.equals(devices, other.devices)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{" + "id=" + N.toString(id) + ", " + "gui=" + N.toString(gui) + ", " + "emailAddress=" + N.toString(emailAddress) + ", " + "firstName="
                + N.toString(firstName) + ", " + "middleName=" + N.toString(middleName) + ", " + "lastName=" + N.toString(lastName) + ", " + "birthDate="
                + N.toString(birthDate) + ", " + "status=" + N.toString(status) + ", " + "lastUpdateTime=" + N.toString(lastUpdateTime) + ", " + "createdTime="
                + N.toString(createdTime) + ", " + "contact=" + N.toString(contact) + ", " + "devices=" + N.toString(devices) + "}";
    }

    /*
     * Auto-generated class for property(field) name table by abacus-jdbc.
     */
    public interface x { // NOSONAR

        /** Property(field) name {@code "id"} */
        String id = "id";

        /** Property(field) name {@code "gui"} */
        String gui = "gui";

        /** Property(field) name {@code "emailAddress"} */
        String emailAddress = "emailAddress";

        /** Property(field) name {@code "firstName"} */
        String firstName = "firstName";

        /** Property(field) name {@code "middleName"} */
        String middleName = "middleName";

        /** Property(field) name {@code "lastName"} */
        String lastName = "lastName";

        /** Property(field) name {@code "birthDate"} */
        String birthDate = "birthDate";

        /** Property(field) name {@code "status"} */
        String status = "status";

        /** Property(field) name {@code "lastUpdateTime"} */
        String lastUpdateTime = "lastUpdateTime";

        /** Property(field) name {@code "createdTime"} */
        String createdTime = "createdTime";

        /** Property(field) name {@code "contact"} */
        String contact = "contact";

        /** Property(field) name {@code "devices"} */
        String devices = "devices";

    }

}
