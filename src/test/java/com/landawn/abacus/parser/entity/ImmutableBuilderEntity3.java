package com.landawn.abacus.parser.entity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Builder
@Data
@Accessors(fluent = true)
public class ImmutableBuilderEntity3 {
    private int id;
    private String firstName;
    private String lastName;
}
