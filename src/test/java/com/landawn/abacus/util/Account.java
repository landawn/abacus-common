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
        h = 31 * h + CommonUtil.hashCode(id);
        h = 31 * h + CommonUtil.hashCode(gui);
        h = 31 * h + CommonUtil.hashCode(emailAddress);
        h = 31 * h + CommonUtil.hashCode(firstName);
        h = 31 * h + CommonUtil.hashCode(middleName);
        h = 31 * h + CommonUtil.hashCode(lastName);
        h = 31 * h + CommonUtil.hashCode(birthDate);
        h = 31 * h + CommonUtil.hashCode(status);
        h = 31 * h + CommonUtil.hashCode(lastUpdateTime);
        h = 31 * h + CommonUtil.hashCode(createdTime);
        h = 31 * h + CommonUtil.hashCode(contact);
        return 31 * h + CommonUtil.hashCode(devices);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Account other) {
            if (CommonUtil.equals(id, other.id) && CommonUtil.equals(gui, other.gui) && CommonUtil.equals(emailAddress, other.emailAddress)
                    && CommonUtil.equals(firstName, other.firstName) && CommonUtil.equals(middleName, other.middleName)
                    && CommonUtil.equals(lastName, other.lastName) && CommonUtil.equals(birthDate, other.birthDate) && CommonUtil.equals(status, other.status)
                    && CommonUtil.equals(lastUpdateTime, other.lastUpdateTime) && CommonUtil.equals(createdTime, other.createdTime)
                    && CommonUtil.equals(contact, other.contact) && CommonUtil.equals(devices, other.devices)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{" + "id=" + CommonUtil.toString(id) + ", " + "gui=" + CommonUtil.toString(gui) + ", " + "emailAddress=" + CommonUtil.toString(emailAddress)
                + ", " + "firstName=" + CommonUtil.toString(firstName) + ", " + "middleName=" + CommonUtil.toString(middleName) + ", " + "lastName="
                + CommonUtil.toString(lastName) + ", " + "birthDate=" + CommonUtil.toString(birthDate) + ", " + "status=" + CommonUtil.toString(status) + ", "
                + "lastUpdateTime=" + CommonUtil.toString(lastUpdateTime) + ", " + "createdTime=" + CommonUtil.toString(createdTime) + ", " + "contact="
                + CommonUtil.toString(contact) + ", " + "devices=" + CommonUtil.toString(devices) + "}";
    }

    public interface x {

        String id = "id";

        String gui = "gui";

        String emailAddress = "emailAddress";

        String firstName = "firstName";

        String middleName = "middleName";

        String lastName = "lastName";

        String birthDate = "birthDate";

        String status = "status";

        String lastUpdateTime = "lastUpdateTime";

        String createdTime = "createdTime";

        String contact = "contact";

        String devices = "devices";

    }

}
