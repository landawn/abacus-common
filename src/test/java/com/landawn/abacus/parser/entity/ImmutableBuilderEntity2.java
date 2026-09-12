package com.landawn.abacus.parser.entity;

import com.landawn.abacus.TestBase;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Value
@Accessors(fluent = true)
public class ImmutableBuilderEntity2 extends TestBase {
    private int id;
    private String firstName;
    private String lastName;
}
