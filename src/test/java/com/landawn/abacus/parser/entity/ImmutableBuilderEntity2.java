package com.landawn.abacus.parser.entity;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;

@Builder
@Value
@Accessors(fluent = true)
public class ImmutableBuilderEntity2 {
    private int id;
    private String firstName;
    private String lastName;
}
