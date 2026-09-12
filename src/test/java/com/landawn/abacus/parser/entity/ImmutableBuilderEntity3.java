package com.landawn.abacus.parser.entity;

import com.landawn.abacus.TestBase;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Builder
@Data
@Accessors(fluent = true)
public class ImmutableBuilderEntity3 extends TestBase {
    private int id;
    private String firstName;
    private String lastName;
}
