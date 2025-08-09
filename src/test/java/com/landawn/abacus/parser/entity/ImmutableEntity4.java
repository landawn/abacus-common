package com.landawn.abacus.parser.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ImmutableEntity4 {
    private int id;
    private String firstName;
    private String lastName;
}
