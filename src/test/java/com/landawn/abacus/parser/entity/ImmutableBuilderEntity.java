package com.landawn.abacus.parser.entity;

import com.landawn.abacus.TestBase;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ImmutableBuilderEntity extends TestBase {
    private final int id;
    private final String firstName;
    private final String lastName;
}
