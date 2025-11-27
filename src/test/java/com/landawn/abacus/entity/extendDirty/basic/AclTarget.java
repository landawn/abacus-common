package com.landawn.abacus.entity.extendDirty.basic;

import java.sql.Timestamp;
import java.util.Objects;

import com.landawn.abacus.annotation.Type;

public class AclTarget implements ExtendDirtyBasicPNL.AclTargetPNL {
    private long id;
    private String gui;
    private String name;
    private String category;
    private String subCategory;
    private String type;
    private String subType;
    private String description;
    private int status;
    private Timestamp lastUpdateTime;
    private Timestamp createdTime;

    public AclTarget() {
    }

    public AclTarget(long id) {
        this();

        setId(id);
    }

    public AclTarget(String gui, String name, String category, String subCategory, String type, String subType, String description, int status,
            Timestamp lastUpdateTime, Timestamp createdTime) {
        this();

        setGUI(gui);
        setName(name);
        setCategory(category);
        setSubCategory(subCategory);
        setType(type);
        setSubType(subType);
        setDescription(description);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreatedTime(createdTime);
    }

    public AclTarget(long id, String gui, String name, String category, String subCategory, String type, String subType, String description, int status,
            Timestamp lastUpdateTime, Timestamp createdTime) {
        this();

        setId(id);
        setGUI(gui);
        setName(name);
        setCategory(category);
        setSubCategory(subCategory);
        setType(type);
        setSubType(subType);
        setDescription(description);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreatedTime(createdTime);
    }

    @Type("long")
    public long getId() {
        return id;
    }

    public AclTarget setId(long id) {
        this.id = id;

        return this;
    }

    @Type("String")
    public String getGUI() {
        return gui;
    }

    public AclTarget setGUI(String gui) {
        this.gui = gui;

        return this;
    }

    @Type("String")
    public String getName() {
        return name;
    }

    public AclTarget setName(String name) {
        this.name = name;

        return this;
    }

    @Type("String")
    public String getCategory() {
        return category;
    }

    public AclTarget setCategory(String category) {
        this.category = category;

        return this;
    }

    @Type("String")
    public String getSubCategory() {
        return subCategory;
    }

    public AclTarget setSubCategory(String subCategory) {
        this.subCategory = subCategory;

        return this;
    }

    @Type("String")
    public String getType() {
        return type;
    }

    public AclTarget setType(String type) {
        this.type = type;

        return this;
    }

    @Type("String")
    public String getSubType() {
        return subType;
    }

    public AclTarget setSubType(String subType) {
        this.subType = subType;

        return this;
    }

    @Type("String")
    public String getDescription() {
        return description;
    }

    public AclTarget setDescription(String description) {
        this.description = description;

        return this;
    }

    @Type("int")
    public int getStatus() {
        return status;
    }

    public AclTarget setStatus(int status) {
        this.status = status;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getLastUpdateTime() {
        return lastUpdateTime;
    }

    public AclTarget setLastUpdateTime(Timestamp lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public AclTarget setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;

        return this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + Objects.hashCode(id);
        h = 31 * h + Objects.hashCode(gui);
        h = 31 * h + Objects.hashCode(name);
        h = 31 * h + Objects.hashCode(category);
        h = 31 * h + Objects.hashCode(subCategory);
        h = 31 * h + Objects.hashCode(type);
        h = 31 * h + Objects.hashCode(subType);
        h = 31 * h + Objects.hashCode(description);
        h = 31 * h + Objects.hashCode(status);
        h = 31 * h + Objects.hashCode(lastUpdateTime);
        return 31 * h + Objects.hashCode(createdTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AclTarget other) {
            return Objects.equals(id, other.id) && Objects.equals(gui, other.gui) && Objects.equals(name, other.name)
                    && Objects.equals(category, other.category) && Objects.equals(subCategory, other.subCategory) && Objects.equals(type, other.type)
                    && Objects.equals(subType, other.subType) && Objects.equals(description, other.description) && Objects.equals(status, other.status)
                    && Objects.equals(lastUpdateTime, other.lastUpdateTime) && Objects.equals(createdTime, other.createdTime);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{id=" + Objects.toString(id) + ", gui=" + Objects.toString(gui) + ", name=" + Objects.toString(name) + ", category="
                + Objects.toString(category) + ", subCategory=" + Objects.toString(subCategory) + ", type=" + Objects.toString(type) + ", subType="
                + Objects.toString(subType) + ", description=" + Objects.toString(description) + ", status=" + Objects.toString(status) + ", lastUpdateTime="
                + Objects.toString(lastUpdateTime) + ", createdTime=" + Objects.toString(createdTime) + "}";
    }
}
