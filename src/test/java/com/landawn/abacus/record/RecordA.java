package com.landawn.abacus.record;

import com.landawn.abacus.annotation.Record;

import lombok.Value;
import lombok.experimental.Accessors;

@Record
@Value
@Accessors(fluent = true)
public class RecordA {

    private final int field1;
    private final String field2;

}
