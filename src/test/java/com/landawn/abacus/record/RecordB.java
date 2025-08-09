package com.landawn.abacus.record;

import com.landawn.abacus.annotation.Entity;

import lombok.Value;
import lombok.experimental.Accessors;

@Entity
@Value
@Accessors(fluent = true)
public class RecordB {

    private final int field1;
    private final String field2;

}
