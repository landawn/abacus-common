package com.landawn.abacus.parser.entity;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class ImmutableBuilderEntity {
    private final int id;
    private final String firstName;
    private final String lastName;
}
