package com.landawn.abacus.entity.extendDirty.basic;

import java.sql.Timestamp;
import java.util.Objects;

import com.landawn.abacus.annotation.Type;

public class AclUgTargetRelationship implements ExtendDirtyBasicPNL.AclUgTargetRelationshipPNL {
    private long id;
    private String ugGui;
    private String targetGui;
    private long privilege;
    private String description;
    private int status;
    private Timestamp lastUpdateTime;
    private Timestamp createdTime;

    public AclUgTargetRelationship() {
    }

    public AclUgTargetRelationship(long id) {
        this();

        setId(id);
    }

    public AclUgTargetRelationship(String ugGui, String targetGui, long privilege, String description, int status, Timestamp lastUpdateTime,
            Timestamp createdTime) {
        this();

        setUgGui(ugGui);
        setTargetGui(targetGui);
        setPrivilege(privilege);
        setDescription(description);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreatedTime(createdTime);
    }

    public AclUgTargetRelationship(long id, String ugGui, String targetGui, long privilege, String description, int status, Timestamp lastUpdateTime,
            Timestamp createdTime) {
        this();

        setId(id);
        setUgGui(ugGui);
        setTargetGui(targetGui);
        setPrivilege(privilege);
        setDescription(description);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreatedTime(createdTime);
    }

    @Type("long")
    public long getId() {
        return id;
    }

    public AclUgTargetRelationship setId(long id) {
        this.id = id;

        return this;
    }

    @Type("String")
    public String getUgGui() {
        return ugGui;
    }

    public AclUgTargetRelationship setUgGui(String ugGui) {
        this.ugGui = ugGui;

        return this;
    }

    @Type("String")
    public String getTargetGui() {
        return targetGui;
    }

    public AclUgTargetRelationship setTargetGui(String targetGui) {
        this.targetGui = targetGui;

        return this;
    }

    @Type("long")
    public long getPrivilege() {
        return privilege;
    }

    public AclUgTargetRelationship setPrivilege(long privilege) {
        this.privilege = privilege;

        return this;
    }

    @Type("String")
    public String getDescription() {
        return description;
    }

    public AclUgTargetRelationship setDescription(String description) {
        this.description = description;

        return this;
    }

    @Type("int")
    public int getStatus() {
        return status;
    }

    public AclUgTargetRelationship setStatus(int status) {
        this.status = status;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getLastUpdateTime() {
        return lastUpdateTime;
    }

    public AclUgTargetRelationship setLastUpdateTime(Timestamp lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public AclUgTargetRelationship setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;

        return this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + Objects.hashCode(id);
        h = 31 * h + Objects.hashCode(ugGui);
        h = 31 * h + Objects.hashCode(targetGui);
        h = 31 * h + Objects.hashCode(privilege);
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

        if (obj instanceof AclUgTargetRelationship other) {
            return Objects.equals(id, other.id) && Objects.equals(ugGui, other.ugGui) && Objects.equals(targetGui, other.targetGui)
                    && Objects.equals(privilege, other.privilege) && Objects.equals(description, other.description) && Objects.equals(status, other.status)
                    && Objects.equals(lastUpdateTime, other.lastUpdateTime) && Objects.equals(createdTime, other.createdTime);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{id=" + Objects.toString(id) + ", ugGui=" + Objects.toString(ugGui) + ", targetGui=" + Objects.toString(targetGui) + ", privilege="
                + Objects.toString(privilege) + ", description=" + Objects.toString(description) + ", status=" + Objects.toString(status) + ", lastUpdateTime="
                + Objects.toString(lastUpdateTime) + ", createdTime=" + Objects.toString(createdTime) + "}";
    }
}
