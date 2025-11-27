package com.landawn.abacus.entity.extendDirty.basic;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Type;

public class Account implements ExtendDirtyBasicPNL.AccountPNL {
    private long id;
    private String gui;
    private String emailAddress;
    @Column("first_name")
    private String firstName;
    private String middleName;
    @AccessFieldByMethod
    private String lastName;
    private Timestamp birthDate;
    private int status;
    private Timestamp lastUpdateTime;
    private Timestamp createdTime;
    private AccountContact contact;
    private List<AccountDevice> devices;

    public Account() {
    }

    public Account(final long id) {
        this();

        setId(id);
    }

    public Account(final String gui, final String emailAddress, final String firstName, final String middleName, final String lastName,
            final Timestamp birthDate, final int status, final Timestamp lastUpdateTime, final Timestamp createdTime, final AccountContact contact,
            final List<AccountDevice> devices) {
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

    public Account(final long id, final String gui, final String emailAddress, final String firstName, final String middleName, final String lastName,
            final Timestamp birthDate, final int status, final Timestamp lastUpdateTime, final Timestamp createdTime, final AccountContact contact,
            final List<AccountDevice> devices) {
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

    @Type("long")
    public long getId() {
        return id;
    }

    public Account setId(final long id) {
        this.id = id;

        return this;
    }

    @Type("String")
    public String getGUI() {
        return gui;
    }

    public Account setGUI(final String gui) {
        this.gui = gui;

        return this;
    }

    @Type("String")
    public String getEmailAddress() {
        return emailAddress;
    }

    public Account setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;

        return this;
    }

    @Type("String")
    public String getFirstName() {
        return firstName;
    }

    public Account setFirstName(final String firstName) {
        this.firstName = firstName;

        return this;
    }

    @Type("String")
    public String getMiddleName() {
        return middleName;
    }

    public Account setMiddleName(final String middleName) {
        this.middleName = middleName;

        return this;
    }

    @Type("String")
    public String getLastName() {
        return lastName;
    }

    public Account setLastName(final String lastName) {
        this.lastName = lastName;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getBirthDate() {
        return birthDate;
    }

    public Account setBirthDate(final Timestamp birthDate) {
        this.birthDate = birthDate;

        return this;
    }

    @Type("int")
    public int getStatus() {
        return status;
    }

    public Account setStatus(final int status) {
        this.status = status;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getLastUpdateTime() {
        return lastUpdateTime;
    }

    public Account setLastUpdateTime(final Timestamp lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public Account setCreatedTime(final Timestamp createdTime) {
        this.createdTime = createdTime;

        return this;
    }

    @Type("com.landawn.abacus.entity.extendDirty.basic.AccountContact")
    public AccountContact getContact() {
        return contact;
    }

    public Account setContact(final AccountContact contact) {
        this.contact = contact;

        return this;
    }

    @Type("List<com.landawn.abacus.entity.extendDirty.basic.AccountDevice>")
    public List<AccountDevice> getDevices() {
        return devices;
    }

    public Account setDevices(final List<AccountDevice> devices) {
        this.devices = devices;

        return this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + Objects.hashCode(id);
        h = 31 * h + Objects.hashCode(gui);
        h = 31 * h + Objects.hashCode(emailAddress);
        h = 31 * h + Objects.hashCode(firstName);
        h = 31 * h + Objects.hashCode(middleName);
        h = 31 * h + Objects.hashCode(lastName);
        h = 31 * h + Objects.hashCode(birthDate);
        h = 31 * h + Objects.hashCode(status);
        h = 31 * h + Objects.hashCode(lastUpdateTime);
        h = 31 * h + Objects.hashCode(createdTime);
        h = 31 * h + Objects.hashCode(contact);
        return 31 * h + Objects.hashCode(devices);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Account other) {
            return Objects.equals(id, other.id) && Objects.equals(gui, other.gui) && Objects.equals(emailAddress, other.emailAddress)
                    && Objects.equals(firstName, other.firstName) && Objects.equals(middleName, other.middleName) && Objects.equals(lastName, other.lastName)
                    && Objects.equals(birthDate, other.birthDate) && Objects.equals(status, other.status)
                    && Objects.equals(lastUpdateTime, other.lastUpdateTime) && Objects.equals(createdTime, other.createdTime)
                    && Objects.equals(contact, other.contact) && Objects.equals(devices, other.devices);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{id=" + Objects.toString(id) + ", gui=" + Objects.toString(gui) + ", emailAddress=" + Objects.toString(emailAddress) + ", firstName="
                + Objects.toString(firstName) + ", middleName=" + Objects.toString(middleName) + ", lastName=" + Objects.toString(lastName) + ", birthDate="
                + Objects.toString(birthDate) + ", status=" + Objects.toString(status) + ", lastUpdateTime=" + Objects.toString(lastUpdateTime)
                + ", createdTime=" + Objects.toString(createdTime) + ", contact=" + Objects.toString(contact) + ", devices=" + Objects.toString(devices) + "}";
    }
}
