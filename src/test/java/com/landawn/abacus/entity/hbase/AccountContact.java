package com.landawn.abacus.entity.hbase;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

import com.landawn.abacus.annotation.Type;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.N;

public class AccountContact implements HbasePNL.AccountContactPNL {
    private HBaseColumn<Long> id;
    private List<HBaseColumn<Long>> accountId;
    private SortedSet<HBaseColumn<String>> telephone;
    private Set<HBaseColumn<String>> city;
    private LinkedHashSet<HBaseColumn<String>> state;
    private List<HBaseColumn<String>> zipCode;
    private List<HBaseColumn<Integer>> status;
    private List<HBaseColumn<Timestamp>> lastUpdateTime;
    private List<HBaseColumn<Timestamp>> createdTime;

    public AccountContact() {
    }

    public AccountContact(HBaseColumn<Long> id) {
        this();

        setId(id);
    }

    public AccountContact(List<HBaseColumn<Long>> accountId, SortedSet<HBaseColumn<String>> telephone, Set<HBaseColumn<String>> city,
            LinkedHashSet<HBaseColumn<String>> state, List<HBaseColumn<String>> zipCode, List<HBaseColumn<Integer>> status,
            List<HBaseColumn<Timestamp>> lastUpdateTime, List<HBaseColumn<Timestamp>> createdTime) {
        this();

        setAccountId(accountId);
        setTelephone(telephone);
        setCity(city);
        setState(state);
        setZipCode(zipCode);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreatedTime(createdTime);
    }

    public AccountContact(HBaseColumn<Long> id, List<HBaseColumn<Long>> accountId, SortedSet<HBaseColumn<String>> telephone, Set<HBaseColumn<String>> city,
            LinkedHashSet<HBaseColumn<String>> state, List<HBaseColumn<String>> zipCode, List<HBaseColumn<Integer>> status,
            List<HBaseColumn<Timestamp>> lastUpdateTime, List<HBaseColumn<Timestamp>> createdTime) {
        this();

        setId(id);
        setAccountId(accountId);
        setTelephone(telephone);
        setCity(city);
        setState(state);
        setZipCode(zipCode);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreatedTime(createdTime);
    }

    @Type("HBaseColumn<Long>")
    public HBaseColumn<Long> getId() {
        return id;
    }

    public AccountContact setId(HBaseColumn<Long> id) {
        this.id = id;

        return this;
    }

    public HBaseColumn<Long> id() {
        return (HBaseColumn<Long>) (this.id == null ? HBaseColumn.emptyOf(long.class) : id);
    }

    public AccountContact setId(long value) {
        setId(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setId(long value, long version) {
        setId(HBaseColumn.valueOf(value, version));

        return this;
    }

    @Type("List<HBaseColumn<Long>>")
    public List<HBaseColumn<Long>> getAccountId() {
        return accountId;
    }

    public AccountContact setAccountId(List<HBaseColumn<Long>> accountId) {
        this.accountId = accountId;

        return this;
    }

    public HBaseColumn<Long> accountId() {
        return (HBaseColumn<Long>) (N.isEmpty(accountId) ? HBaseColumn.emptyOf(long.class) : accountId.iterator().next());
    }

    public AccountContact setAccountId(long value) {
        setAccountId(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setAccountId(long value, long version) {
        setAccountId(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact setAccountId(HBaseColumn<Long> hbaseColumn) {
        if (accountId == null) {
            accountId = N.newInstance(java.util.List.class);
        } else {
            accountId.clear();
        }

        accountId.add(hbaseColumn);

        return this;
    }

    public AccountContact addAccountId(long value) {
        addAccountId(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact addAccountId(long value, long version) {
        addAccountId(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact addAccountId(HBaseColumn<Long> hbaseColumn) {
        if (accountId == null) {
            accountId = N.newInstance(java.util.List.class);
        }

        accountId.add(hbaseColumn);

        return this;
    }

    @Type("SortedSet<HBaseColumn<String>>")
    public SortedSet<HBaseColumn<String>> getTelephone() {
        return telephone;
    }

    public AccountContact setTelephone(SortedSet<HBaseColumn<String>> telephone) {
        this.telephone = telephone;

        return this;
    }

    public HBaseColumn<String> telephone() {
        return (HBaseColumn<String>) (N.isEmpty(telephone) ? HBaseColumn.emptyOf(String.class) : telephone.iterator().next());
    }

    public AccountContact setTelephone(String value) {
        setTelephone(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setTelephone(String value, long version) {
        setTelephone(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact setTelephone(HBaseColumn<String> hbaseColumn) {
        if (telephone == null) {
            telephone = new java.util.TreeSet<>(HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR);
        } else {
            telephone.clear();
        }

        telephone.add(hbaseColumn);

        return this;
    }

    public AccountContact addTelephone(String value) {
        addTelephone(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact addTelephone(String value, long version) {
        addTelephone(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact addTelephone(HBaseColumn<String> hbaseColumn) {
        if (telephone == null) {
            telephone = new java.util.TreeSet<>(HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR);
        }

        telephone.add(hbaseColumn);

        return this;
    }

    @Type("Set<HBaseColumn<String>>")
    public Set<HBaseColumn<String>> getCity() {
        return city;
    }

    public AccountContact setCity(Set<HBaseColumn<String>> city) {
        this.city = city;

        return this;
    }

    public HBaseColumn<String> city() {
        return (HBaseColumn<String>) (N.isEmpty(city) ? HBaseColumn.emptyOf(String.class) : city.iterator().next());
    }

    public AccountContact setCity(String value) {
        setCity(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setCity(String value, long version) {
        setCity(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact setCity(HBaseColumn<String> hbaseColumn) {
        if (city == null) {
            city = N.newInstance(java.util.Set.class);
        } else {
            city.clear();
        }

        city.add(hbaseColumn);

        return this;
    }

    public AccountContact addCity(String value) {
        addCity(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact addCity(String value, long version) {
        addCity(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact addCity(HBaseColumn<String> hbaseColumn) {
        if (city == null) {
            city = N.newInstance(java.util.Set.class);
        }

        city.add(hbaseColumn);

        return this;
    }

    @Type("LinkedHashSet<HBaseColumn<String>>")
    public LinkedHashSet<HBaseColumn<String>> getState() {
        return state;
    }

    public AccountContact setState(LinkedHashSet<HBaseColumn<String>> state) {
        this.state = state;

        return this;
    }

    public HBaseColumn<String> state() {
        return (HBaseColumn<String>) (N.isEmpty(state) ? HBaseColumn.emptyOf(String.class) : state.iterator().next());
    }

    public AccountContact setState(String value) {
        setState(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setState(String value, long version) {
        setState(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact setState(HBaseColumn<String> hbaseColumn) {
        if (state == null) {
            state = N.newInstance(java.util.LinkedHashSet.class);
        } else {
            state.clear();
        }

        state.add(hbaseColumn);

        return this;
    }

    public AccountContact addState(String value) {
        addState(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact addState(String value, long version) {
        addState(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact addState(HBaseColumn<String> hbaseColumn) {
        if (state == null) {
            state = N.newInstance(java.util.LinkedHashSet.class);
        }

        state.add(hbaseColumn);

        return this;
    }

    @Type("List<HBaseColumn<String>>")
    public List<HBaseColumn<String>> getZipCode() {
        return zipCode;
    }

    public AccountContact setZipCode(List<HBaseColumn<String>> zipCode) {
        this.zipCode = zipCode;

        return this;
    }

    public HBaseColumn<String> zipCode() {
        return (HBaseColumn<String>) (N.isEmpty(zipCode) ? HBaseColumn.emptyOf(String.class) : zipCode.iterator().next());
    }

    public AccountContact setZipCode(String value) {
        setZipCode(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setZipCode(String value, long version) {
        setZipCode(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact setZipCode(HBaseColumn<String> hbaseColumn) {
        if (zipCode == null) {
            zipCode = N.newInstance(java.util.List.class);
        } else {
            zipCode.clear();
        }

        zipCode.add(hbaseColumn);

        return this;
    }

    public AccountContact addZipCode(String value) {
        addZipCode(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact addZipCode(String value, long version) {
        addZipCode(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact addZipCode(HBaseColumn<String> hbaseColumn) {
        if (zipCode == null) {
            zipCode = N.newInstance(java.util.List.class);
        }

        zipCode.add(hbaseColumn);

        return this;
    }

    @Type("List<HBaseColumn<Integer>>")
    public List<HBaseColumn<Integer>> getStatus() {
        return status;
    }

    public AccountContact setStatus(List<HBaseColumn<Integer>> status) {
        this.status = status;

        return this;
    }

    public HBaseColumn<Integer> status() {
        return (HBaseColumn<Integer>) (N.isEmpty(status) ? HBaseColumn.emptyOf(int.class) : status.iterator().next());
    }

    public AccountContact setStatus(int value) {
        setStatus(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setStatus(int value, long version) {
        setStatus(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact setStatus(HBaseColumn<Integer> hbaseColumn) {
        if (status == null) {
            status = N.newInstance(java.util.List.class);
        } else {
            status.clear();
        }

        status.add(hbaseColumn);

        return this;
    }

    public AccountContact addStatus(int value) {
        addStatus(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact addStatus(int value, long version) {
        addStatus(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact addStatus(HBaseColumn<Integer> hbaseColumn) {
        if (status == null) {
            status = N.newInstance(java.util.List.class);
        }

        status.add(hbaseColumn);

        return this;
    }

    @Type("List<HBaseColumn<Timestamp>>")
    public List<HBaseColumn<Timestamp>> getLastUpdateTime() {
        return lastUpdateTime;
    }

    public AccountContact setLastUpdateTime(List<HBaseColumn<Timestamp>> lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;

        return this;
    }

    public HBaseColumn<Timestamp> lastUpdateTime() {
        return (HBaseColumn<Timestamp>) (N.isEmpty(lastUpdateTime) ? HBaseColumn.emptyOf(Timestamp.class) : lastUpdateTime.iterator().next());
    }

    public AccountContact setLastUpdateTime(Timestamp value) {
        setLastUpdateTime(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setLastUpdateTime(Timestamp value, long version) {
        setLastUpdateTime(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact setLastUpdateTime(HBaseColumn<Timestamp> hbaseColumn) {
        if (lastUpdateTime == null) {
            lastUpdateTime = N.newInstance(java.util.List.class);
        } else {
            lastUpdateTime.clear();
        }

        lastUpdateTime.add(hbaseColumn);

        return this;
    }

    public AccountContact addLastUpdateTime(Timestamp value) {
        addLastUpdateTime(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact addLastUpdateTime(Timestamp value, long version) {
        addLastUpdateTime(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact addLastUpdateTime(HBaseColumn<Timestamp> hbaseColumn) {
        if (lastUpdateTime == null) {
            lastUpdateTime = N.newInstance(java.util.List.class);
        }

        lastUpdateTime.add(hbaseColumn);

        return this;
    }

    @Type("List<HBaseColumn<Timestamp>>")
    public List<HBaseColumn<Timestamp>> getCreatedTime() {
        return createdTime;
    }

    public AccountContact setCreatedTime(List<HBaseColumn<Timestamp>> createdTime) {
        this.createdTime = createdTime;

        return this;
    }

    public HBaseColumn<Timestamp> createdTime() {
        return (HBaseColumn<Timestamp>) (N.isEmpty(createdTime) ? HBaseColumn.emptyOf(Timestamp.class) : createdTime.iterator().next());
    }

    public AccountContact setCreatedTime(Timestamp value) {
        setCreatedTime(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact setCreatedTime(Timestamp value, long version) {
        setCreatedTime(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact setCreatedTime(HBaseColumn<Timestamp> hbaseColumn) {
        if (createdTime == null) {
            createdTime = N.newInstance(java.util.List.class);
        } else {
            createdTime.clear();
        }

        createdTime.add(hbaseColumn);

        return this;
    }

    public AccountContact addCreatedTime(Timestamp value) {
        addCreatedTime(HBaseColumn.valueOf(value));

        return this;
    }

    public AccountContact addCreatedTime(Timestamp value, long version) {
        addCreatedTime(HBaseColumn.valueOf(value, version));

        return this;
    }

    public AccountContact addCreatedTime(HBaseColumn<Timestamp> hbaseColumn) {
        if (createdTime == null) {
            createdTime = N.newInstance(java.util.List.class);
        }

        createdTime.add(hbaseColumn);

        return this;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + Objects.hashCode(id);
        h = 31 * h + Objects.hashCode(accountId);
        h = 31 * h + Objects.hashCode(telephone);
        h = 31 * h + Objects.hashCode(city);
        h = 31 * h + Objects.hashCode(state);
        h = 31 * h + Objects.hashCode(zipCode);
        h = 31 * h + Objects.hashCode(status);
        h = 31 * h + Objects.hashCode(lastUpdateTime);
        return 31 * h + Objects.hashCode(createdTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AccountContact other) {
            return Objects.equals(id, other.id) && Objects.equals(accountId, other.accountId) && Objects.equals(telephone, other.telephone)
                    && Objects.equals(city, other.city) && Objects.equals(state, other.state) && Objects.equals(zipCode, other.zipCode)
                    && Objects.equals(status, other.status) && Objects.equals(lastUpdateTime, other.lastUpdateTime)
                    && Objects.equals(createdTime, other.createdTime);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{id=" + Objects.toString(id) + ", accountId=" + Objects.toString(accountId) + ", telephone=" + Objects.toString(telephone) + ", city="
                + Objects.toString(city) + ", state=" + Objects.toString(state) + ", zipCode=" + Objects.toString(zipCode) + ", status="
                + Objects.toString(status) + ", lastUpdateTime=" + Objects.toString(lastUpdateTime) + ", createdTime=" + Objects.toString(createdTime) + "}";
    }
}
