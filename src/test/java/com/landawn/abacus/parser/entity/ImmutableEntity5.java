package com.landawn.abacus.parser.entity;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class ImmutableEntity5 {
    private int id;
    private String firstName;
    private String lastName;
}
